package lila.oidc

import play.api.libs.json.*

class PlatformSignatureTest extends munit.FunSuite:

  private val allowed = PlatformProvider.all.toSet

  test("provider values are stable") {
    assertEquals(PlatformProvider.all.map(_.value), List("yandex-game", "vk-game", "friday-game"))
  }

  test("unknown provider is rejected") {
    assertEquals(PlatformProvider.from("steam"), None)
    assertEquals(
      PlatformSignature.validate("steam", "sig", allowed),
      Left(SignatureError.UnknownProvider)
    )
  }

  test("provider outside the allowlist is rejected") {
    assertEquals(
      PlatformSignature.validate("vk-game", "sig", Set(PlatformProvider.YandexGame)),
      Left(SignatureError.UnknownProvider)
    )
  }

  test("empty and oversized signatures are rejected") {
    assertEquals(
      PlatformSignature.validate("yandex-game", "  ", allowed),
      Left(SignatureError.InvalidRequest)
    )
    assertEquals(
      PlatformSignature.validate("yandex-game", "x" * (PlatformSignature.MaxSignatureLength + 1), allowed),
      Left(SignatureError.InvalidRequest)
    )
  }

  test("valid signature is trimmed and returned with its provider") {
    assertEquals(
      PlatformSignature.validate("yandex-game", "  abc  ", allowed),
      Right(PlatformProvider.YandexGame -> "abc")
    )
  }

  test("request JSON requires provider and signature strings") {
    assertEquals(SignatureRequest.fromJson(Json.obj()), Left(SignatureError.InvalidRequest))
    assertEquals(
      SignatureRequest.fromJson(Json.obj("provider" -> "  ", "signature" -> "abc")),
      Left(SignatureError.InvalidRequest)
    )
    assertEquals(
      SignatureRequest.fromJson(Json.obj("provider" -> "yandex-game", "signature" -> 12)),
      Left(SignatureError.InvalidRequest)
    )
    assertEquals(SignatureRequest.fromJson(Json.arr()), Left(SignatureError.InvalidRequest))
    assertEquals(
      SignatureRequest.fromJson(Json.obj("provider" -> " yandex-game ", "signature" -> "abc")),
      Right(SignatureRequest("yandex-game", "abc"))
    )
  }

  test("JSON validation applies provider allowlist and signature limits") {
    val request = Json.obj("provider" -> "vk-game", "signature" -> "abc")
    assertEquals(
      PlatformSignature.validateJson(request, Set(PlatformProvider.YandexGame)),
      Left(SignatureError.UnknownProvider)
    )
    assertEquals(
      PlatformSignature.validateJson(request, allowed),
      Right(PlatformProvider.VkGame -> "abc")
    )
    assertEquals(
      PlatformSignature.validateJson(
        Json.obj("provider" -> "vk-game", "signature" -> " "),
        allowed
      ),
      Left(SignatureError.InvalidRequest)
    )
  }
