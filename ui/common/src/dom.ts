
export const getElementsByClassName = (element: Element, classNAME: string): Element[] => { // returns Array
    return [].slice.call(element['getElementsByClassName'](classNAME));
}

export const hasClass = (el: HTMLElement|null, className: string) => {
    if (el) {
        return el.classList ? el.classList.contains(className) : new RegExp('\\b'+ className+'\\b').test(el.className);
    } else {
        return false;
    }
}

export const addClass = (el: HTMLElement|null, className: string) => {
    if (el) {
        if (el.classList) {
            el.classList.add(className);
        } else if (!hasClass(el, className)) {
            el.className += ' ' + className;
        }
    }
}

export const removeClass = (el: HTMLElement|null, className: string) => {
    if (el) {
        if (el.classList) {
            el.classList.remove(className);
        } else {
            el.className = el.className.replace(new RegExp('\\b'+ className+'\\b', 'g'), '');
        }
    }
}

export const toggleClass = (el: HTMLElement|null, className: string) => {
    if (hasClass(el, className)){
        removeClass(el, className)
    } else {
        addClass(el, className)
    }
}
