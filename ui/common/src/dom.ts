export const getElementsByClassName = (el: Element, classNAME: string): HTMLElement[] => {
  // returns Array
  return [].slice.call(el['getElementsByClassName'](classNAME));
};

export const hasClass = (el: HTMLElement | null, className: string): boolean => {
  if (el) {
    return el.classList
      ? el.classList.contains(className)
      : new RegExp('\\b' + className + '\\b').test(el.className);
  } else {
    return false;
  }
};

export const addClass = (el: HTMLElement | null, className: string): void => {
  if (el) {
    if (el.classList) {
      el.classList.add(className);
    } else if (!hasClass(el, className)) {
      el.className += ' ' + className;
    }
  }
};

export const removeClass = (el: HTMLElement | null, className: string): void => {
  if (el) {
    if (el.classList) {
      el.classList.remove(className);
    } else {
      el.className = el.className.replace(new RegExp('\\b' + className + '\\b', 'g'), '');
    }
  }
};

export const toggleClass = (el: HTMLElement | null, className: string): void => {
  if (hasClass(el, className)) {
    removeClass(el, className);
  } else {
    addClass(el, className);
  }
};
