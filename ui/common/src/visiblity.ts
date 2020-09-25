
export type DeviceSizeType = 'xlg' | 'xl' | 'lg' | 'md' | 'sm' | 'xs';

/**
 * 
 * @param size Check visiblity for device size
 */
export const checkVisiblity = (size: DeviceSizeType) => {
    const elementId = "pg-visible-" + size;
    const elementClass = "visible-" + size + '-only';

    let pgElement = document.getElementById(elementId);
    
    if (!pgElement) {
        const utilElement = document.createElement('div');
        utilElement.className = elementClass;
        utilElement.setAttribute("id", elementId);
        document.body.appendChild(utilElement)
        pgElement = document.getElementById(elementId);
    }

    return (pgElement!.offsetWidth === 0 && pgElement!.offsetHeight === 0) ? false : true;
}

/** 
 * @function isVisibleXs
 * @description Checks if the screen size is XS - Extra Small i.e below W480px
 * @returns boolean
 */
export const isVisibleXs = (): boolean => {
    return checkVisiblity('xs');
}

/** 
 * @function isVisibleSm
 * @description Checks if the screen size is SM - Small Screen i.e Above W480px
 * @returns boolean
 */
export const isVisibleSm = (): boolean => {
    return checkVisiblity('sm');
}

/** 
 * @function isVisibleMd
 * @description Checks if the screen size is MD - Medium Screen i.e Above W1024px
 * @returns boolean
 */
export const isVisibleMd = (): boolean => {
    return checkVisiblity('md');
}

/** 
 * @function isVisibleLg
 * @description Checks if the screen size is LG - Large Screen i.e Above W1200px
 * @returns boolean
 */
export const isVisibleLg = (): boolean => {
    return checkVisiblity('lg');
}

/** 
 * @function isVisibleXl
 * @description Checks if the screen size is XL - Extra Large Screen
 * @returns boolean
 */
export const isVisibleXl = (): boolean => {
    return checkVisiblity('xl');
}

/** 
 * @function isVisibleXlg
 * @description Checks if the screen size is XLG - Extra Extra Large Screen
 * @returns boolean
 */
export const isVisibleXlg = (): boolean => {
    return checkVisiblity('xlg');
}

export const isVisible = (element: HTMLElement): boolean => {
    return (element.offsetWidth > 0 || element.offsetHeight > 0)
}