import * as dom from '../../../common/src/dom';

interface Poster  {
    id: number;
    name: string;
    display: string;
    aurl: string;
}

interface UpdateData {
    fid: number;
    tid: number;
    pid: number;
    title: string;
    forum: string;
    time: string;
    url: string;
    poster: Poster;
}

const forumTop = {
    init(node: HTMLElement) {
        const boxes = dom.getElementsByClassName(node, 'lobby__box__top');
        if (boxes.length > 0) {
            const box = <HTMLElement>boxes[0];
            const contents = dom.getElementsByClassName(box, 'lobby__box__content');
            if (contents.length > 0) {
                const content = <HTMLElement>contents[0];


            }
        }
    },
    update(node: HTMLElement, data: UpdateData[]) {
        const ol = document.createElement('ol');
        data.forEach(item => {
            const li = document.createElement('li');
            const title = document.createElement('a');
            title.innerText = item.forum + ': ' + item.title;
            title.href = item.url;
            dom.addClass(title, 'post_link');
            dom.addClass(title, 'text');

            const author = document.createElement('a');
            author.innerText = item.poster.name;
            author.href = '/@/' + item.poster.id;
            dom.addClass(author, 'user-link');
            dom.addClass(author, 'ulpt');

            const extra = document.createElement('span');
            extra.innerText = item.time;
            dom.addClass(extra, 'extract');

            li.append(title);
            li.append(author);
            li.append(extra);

            ol.appendChild(li);
        }); 

        const old = node.childNodes[0];
        node.replaceChild(ol, old);
    }
}

export default forumTop;