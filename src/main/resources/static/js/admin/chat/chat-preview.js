window.updateChatPreview = function () {
    const chatContainer = document.getElementById('preview-chat-container');
    if (!chatContainer) return;
    chatContainer.innerHTML = '';

    const nameA = document.getElementById('char-a-rep')?.value || 'A';
    const nameB = document.getElementById('char-b-rep')?.value || 'B';
    const charAImg = document.getElementById('char-a-img-bg')?.style.backgroundImage || '';
    const charBImg = document.getElementById('char-b-img-bg')?.style.backgroundImage || '';

    // 섹션별 대사 렌더링 헬퍼
    const renderBlocks = (containerSelector, sectionTitle = null) => {
        const blocks = document.querySelectorAll(`${containerSelector} .script-block`);

        if (blocks.length > 0 && sectionTitle) {
            chatContainer.innerHTML += `<div class="text-[9px] font-bold text-purple-500 bg-purple-50 py-1 px-3 rounded-full w-fit mx-auto my-4 border border-purple-100">${sectionTitle}</div>`;
        }

        blocks.forEach(block => {
            // 화자 타입 가져오기 (A, B, NARRATOR)
            const type = block.querySelector('.speaker-select')?.value || block.dataset.speaker;
            const ta = block.querySelector('.script-text');
            if (!ta) return;

            // [태그] 숨김 처리 및 줄바꿈 변환
            let text = ta.value.replace(/\[.*?\]/g, '').replace(/<[^>]+>/g, '').trim();
            if (!text) return;
            text = text.replace(/\n/g, '<br>');

            // 화자 타입에 따라 말풍선 생성
            chatContainer.innerHTML += _buildBubble(type, text, nameA, nameB, charAImg, charBImg);
        });
    };

    // 1. 시작 노드 (오프닝)
    renderBlocks('#start-node-container');

    // 2. 분기점 처리
    const branchContainer = document.getElementById('branch-container');
    const branchChoiceUI = document.getElementById('preview-branch-choice');

    if (branchContainer && !branchContainer.classList.contains('hidden')) {
        if (branchChoiceUI) branchChoiceUI.classList.remove('hidden');

        const labelA = document.getElementById('branch-a-label')?.value || 'A 선택지';
        const labelB = document.getElementById('branch-b-label')?.value || 'B 선택지';

        if (document.getElementById('branch-btn-a')) document.getElementById('branch-btn-a').innerHTML = labelA.replace(/\n/g, '<br>');
        if (document.getElementById('branch-btn-b')) document.getElementById('branch-btn-b').innerHTML = labelB.replace(/\n/g, '<br>');

        renderBlocks('#branch-a-node-container', 'OPTION A PATH');
        renderBlocks('#branch-b-node-container', 'OPTION B PATH');
    } else {
        if (branchChoiceUI) branchChoiceUI.classList.add('hidden');
    }

    // 3. 클로징 노드
    renderBlocks('#closing-node-container');

    chatContainer.scrollTop = chatContainer.scrollHeight;
};

// 말풍선 HTML 생성 헬퍼
function _buildBubble(type, text, nameA, nameB, charAImg, charBImg) {
    if (type === 'NARRATOR') {
        return `
            <div class="my-4 px-4 text-center">
                <div class="bg-[#F8F8F8] border border-[#EEEEEE] rounded-xl p-3 inline-block max-w-[90%]">
                    <p class="text-[11px] text-gray-500 leading-relaxed font-medium">${text}</p>
                </div>
            </div>`;
    }

    const isA = (type === 'A');
    const name = isA ? nameA : nameB;
    const img = isA ? charAImg : charBImg;
    const alignClass = isA ? '' : 'flex-row-reverse';
    const textAlign = isA ? '' : 'items-end';
    const namePadding = isA ? 'pl-1' : 'pr-1';
    const bubbleClass = isA
        ? 'bg-white border-[#EBEBEB] text-gray-800 rounded-tl-none'
        : 'bg-[#FDFBF9] border-[#EBE2D5] text-[#5A4A35] rounded-tr-none';

    return `
        <div class="flex items-start gap-2 mb-4 px-2 ${alignClass}">
            <div class="w-8 h-8 rounded-full flex-none bg-cover bg-center border border-gray-100" 
                 style="${img ? 'background-image:' + img : 'background-color:#eee'}"></div>
            <div class="max-w-[80%] flex flex-col ${textAlign}">
                <p class="text-[10px] font-bold text-gray-400 mb-1 ${namePadding}">${name}</p>
                <div class="border text-[12px] px-3.5 py-3 rounded-2xl shadow-[0_2px_4px_rgba(0,0,0,0.02)] leading-relaxed ${bubbleClass}">
                    ${text}
                </div>
            </div>
        </div>`;
}

// 화면 전환 핸들러
window.switchToChatView = function () {
    const intro = document.getElementById('preview-battle-intro');
    const chat = document.getElementById('preview-battle-chat');
    if (intro && chat) {
        intro.classList.add('hidden');
        chat.classList.remove('hidden');
        window.updateChatPreview();
    }
};

window.switchToIntroView = function () {
    const intro = document.getElementById('preview-battle-intro');
    const chat = document.getElementById('preview-battle-chat');
    if (intro && chat) {
        chat.classList.add('hidden');
        intro.classList.remove('hidden');
    }
};

window.resetChatPreview = function () {
    window.updateChatPreview();
};