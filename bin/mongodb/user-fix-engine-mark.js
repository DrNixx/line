function in_array(needle, haystack) {
    var found = false, key, strict = !!strict;
    for (key in haystack) {
        if ((haystack[key] == needle)) {
            found = true;
            break;
        }
    }
    return found;
}


db.user4.find({"rankban": true}).forEach(function(u) {

    var marks = [];

    if (u.marks) {
        marks = u.marks;
    }

    if (!in_array('rankban', marks)) {
        marks.push("rankban");
        db.user4.update({_id: u._id}, { $unset: { rankban: true }});
        db.user4.update({_id: u._id}, { $set: { marks: marks }});
        print(`${u._id} ${u.username}`);
    }
});


