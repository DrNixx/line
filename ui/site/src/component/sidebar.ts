import { extend } from "common/common";
import * as dom from "common/dom";
import { isVisibleXs, isVisibleSm } from "common/visiblity";
import { storage } from './storage';

interface ISideBarOptions {
    pageContainer?: string;
    cssAnimation?: boolean;
    sideBarWidthCondensed?: number;
    sideBarWidth?: number;
    css3d?: boolean;
}

const defaultProps: ISideBarOptions = {
    pageContainer :".page-container",
    cssAnimation: true,
    css3d: true,
    sideBarWidth: 280,
    sideBarWidthCondensed: 280 - 70
};

const 
    sShow       = 'show',
    sHide       = 'hide',
    sOpen       = 'open',
    sActive     = 'active';

const body = document.body;

const sidebar = () => {
    const pinnedStorage = storage.makeBoolean('sidebar.pinned');

    const pin = pinnedStorage.get();
    if (pin) {
        dom.addClass(body, 'menu-pin');
    } else {
        dom.removeClass(body, 'menu-pin');
    }

    const sidebarElements = document.querySelectorAll('[data-pages="sidebar"]');

    [].forEach.call(sidebarElements, function(element: HTMLElement) {

        const options = extend(defaultProps, element.dataset);

        const $el = $(element);

        const openSideBar = () => {
            var _sideBarWidthCondensed = dom.hasClass(body, "rtl") ? - options.sideBarWidthCondensed! : options.sideBarWidthCondensed;
    
             var menuOpenCSS = options.css3d == true ? 
                'translate3d(' + _sideBarWidthCondensed + 'px, 0,0)' : 
                'translate(' + _sideBarWidthCondensed + 'px, 0)';
    
             if (isVisibleSm() || isVisibleXs()) {
                 return;
             }
    
             // @TODO : 
             // if ($('.close-sidebar').data('clicked')) {
             //     return;
             // }
             if (dom.hasClass(body, "menu-pin")) {
                return;
             }
    
             element.style.transform = menuOpenCSS;
             dom.addClass(body, 'sidebar-visible');
        }

        const closeSideBar = () => {
            var menuClosedCSS = options.css3d == true ? 
                'translate3d(0, 0,0)' : 
                'translate(0, 0)';
    
             if (isVisibleSm() || isVisibleXs()) {
                 return;
             }
             // @TODO : 
             // if (typeof e != 'undefined') {
             //     if (document.querySelectorAll('.page-sidebar').length) {
             //         return;
             //     }
             // }
             if (dom.hasClass(body,"menu-pin"))
                 return;
    
             if (dom.hasClass(element.querySelector('.sidebar-overlay-slide'), sShow)) {
                // @TODO : 
                dom.removeClass(element.querySelector('.sidebar-overlay-slide'), sShow);
                // $("[data-pages-toggle']").removeClass(sActive)
             }
    
             element.style.transform = menuClosedCSS;
             dom.removeClass(body,'sidebar-visible');
        }

        const toggleSidebar = () => {
            let timer;
            const bodyStyles = getComputedStyle(body, null);
            const pageContainer = <HTMLElement>document.querySelectorAll(options.pageContainer!)[0];
            pageContainer.style.backgroundColor = bodyStyles.backgroundColor;
    
            if (dom.hasClass(body,'sidebar-' + sOpen)) {
                dom.removeClass(body,'sidebar-' + sOpen);
                timer = setTimeout(function() {
                     dom.removeClass(self.element,'visible');
                }.bind(self), 400);
            } else {
                clearTimeout(timer);
                dom.addClass(self.element,'visible');
                setTimeout(function() {
                     dom.addClass(body,'sidebar-' + sOpen);
                }.bind(self), 10);
    
                setTimeout(function() {
                     // remove background color
                     self.pageContainer.style.backgroundColor = ''
                }, 1000);
            }
        }

        const togglePinSidebar = (toggle?: string) => {
            if (toggle == sHide) {
                dom.removeClass(body, 'menu-pin');
            } else if (toggle == sShow) {
                dom.addClass(body, 'menu-pin');
            } else {
                dom.toggleClass(body, 'menu-pin');
             }
        };

        const handleMenu = (e: JQueryEventObject) => {
            const element = <HTMLAnchorElement>e.currentTarget;
            const li = <HTMLLIElement>element.parentNode;

            if (!li.querySelectorAll(".sub-menu")) {
                return;
            }

            const parent = <HTMLElement>li.parentNode;
            const sub = <HTMLElement>li.querySelector(".sub-menu");
            if (dom.hasClass(li, sOpen)) {
                dom.removeClass(element.querySelector(".arrow"), sOpen)
                dom.removeClass(element.querySelector(".arrow"), sActive);
                if (sub) {
                    dom.removeClass(li, sOpen);
                    dom.removeClass(li, sActive);
                }
            } else {
                const openMenu = <HTMLLIElement>parent.querySelector("li." + sOpen);
                if (openMenu) {
                    const openMenuSub = <HTMLElement>openMenu.querySelector(".sub-menu");
                    dom.removeClass(openMenuSub, sOpen)
                    dom.removeClass(openMenuSub, sActive)
                    dom.removeClass(openMenu, sOpen);
                    dom.removeClass(openMenu, sActive);
                    dom.removeClass(openMenu.querySelector("li > a .arrow"), sOpen);
                    dom.removeClass(openMenu.querySelector("li > a .arrow"), sActive);
                }
                
                dom.addClass(element.querySelector(".arrow"), sOpen);
                dom.addClass(element.querySelector(".arrow"), sActive);
                if (sub) {
                    dom.addClass(li, sOpen)
                    dom.addClass(li, sActive)
                }
            }
        };

        $el.on('mouseenter', openSideBar).on('mouseleave', closeSideBar);
        if ('ontouchstart' in document.documentElement) {
            $el.on('ontouchstart', openSideBar);
        }

        // add handler for menu toggler with attr "data-toggle" equal to data-pages
        var dp = element.getAttribute("data-pages");
        if (dp) {
            $('[data-toggle="' + dp + '"]').on('click', function(e: JQueryEventObject) {
                e.preventDefault();
                toggleSidebar();
            });
        }

        $('.sidebar-slide-toggle').on('click', function(e: JQueryEventObject) {
            e.preventDefault();
            dom.toggleClass(<HTMLElement>e.currentTarget, sActive);
            const elId = e.currentTarget.getAttribute('data-pages-toggle');
            if (elId != null) {
                //Only by ID
                const el = document.getElementById(elId.substr(1));
                dom.toggleClass(<HTMLElement>el, sShow);
            }
        });

        $('[data-toggle-pin="sidebar"]').on('click', function(e) {
            e.preventDefault();
            const pin = pinnedStorage.get();
            const action = pin ? sHide : sShow;
            togglePinSidebar(action);
            pinnedStorage.set(!pin);
        });

        $('.sidebar-menu a').on('click', handleMenu);
    });
}

export default sidebar;