// 대본 입력창 자동 높이
const resizeScriptTextarea = (textarea) => {
    if (!textarea) return;
    textarea.style.height = 'auto';
    textarea.style.overflowY = 'hidden';
    textarea.style.height = `${textarea.scrollHeight}px`;
};

const bindScriptTextareaAutosize = (textarea) => {
    if (!textarea) return;
    textarea.addEventListener('input', () => {
        resizeScriptTextarea(textarea);
        if (window.updateChatPreview) window.updateChatPreview();
    });
    resizeScriptTextarea(textarea);
};

const initScriptTextareaAutosize = () => {
    document.querySelectorAll('.script-text').forEach(bindScriptTextareaAutosize);
};

window.addScriptBlock = (containerId, speaker) => {
    const block = document.createElement('div');
    block.className = 'flex items-start gap-4 script-block bg-white border border-gray-100 p-4 rounded-2xl shadow-sm mb-3 group';
    block.innerHTML = `
        <input type="hidden" class="mod-flag" name="modifiedBlocks[]" value="false">
        <div class="flex flex-col gap-2 flex-none w-24">
            <select class="border border-gray-200 rounded-lg text-[11px] p-2 font-bold speaker-select bg-gray-50 outline-none">
                <option value="A" ${speaker === 'A' ? 'selected' : ''}>화자 A</option>
                <option value="B" ${speaker === 'B' ? 'selected' : ''}>화자 B</option>
                <option value="NARRATOR" ${speaker === 'NARRATOR' ? 'selected' : ''}>내레이터</option>
            </select>
            <select class="border border-purple-200 rounded-lg text-[10px] p-2 font-bold bg-purple-50 emotion-insert-btn text-purple-600 outline-none">
                <option value="">+ 감정 태그 삽입</option>
                <optgroup label="브레이크 효과 (기본)">
                    <option value="pause">짧게 쉬기 [pause]</option>
                    <option value="long pause">길게 쉬기 [long pause]</option>
                    <option value="clear throat">헛기침 [clear throat]</option>
                    <option value="sighing">한숨 [sighing]</option>
                </optgroup>
                <optgroup label="감정 표현">
                    <option value="angry">분노 [angry]</option>
                    <option value="sad">슬픔 [sad]</option>
                    <option value="embarrassed">당황 [embarrassed]</option>
                    <option value="emphasis">강조 [emphasis]</option>
                    <option value="whispering">속삭임 [whispering]</option>
                    <option value="soft">부드럽게 [soft]</option>
                    <option value="breathy">숨소리 [breathy]</option>
                    <option value="excited">흥분 [excited]</option>
                </optgroup>
                <optgroup label="웃음/효과음">
                    <option value="laughing">웃음 [laughing]</option>
                    <option value="chuckling">가벼운 웃음 [chuckling]</option>
                    <option value="moaning">신음 [moaning]</option>
                    <option value="sobbing">흐느낌 [sobbing]</option>
                    <option value="crying loudly">큰 울음 [crying loudly]</option>
                    <option value="panting">헐떡임 [panting]</option>
                    <option value="groaning">앓는 소리 [groaning]</option>
                    <option value="crowd laughing">군중 웃음 [crowd laughing]</option>
                    <option value="background laughter">배경 웃음 [background laughter]</option>
                    <option value="audience laughing">관객 웃음 [audience laughing]</option>
                </optgroup>
            </select>
        </div>
        <textarea rows="1" class="flex-1 text-sm script-text outline-none resize-none py-1 leading-relaxed overflow-hidden" placeholder="대사를 입력하세요..."></textarea>
        <button onclick="this.parentElement.remove(); if(window.updateChatPreview) window.updateChatPreview();" class="text-gray-300 hover:text-red-500 font-bold p-2">&times;</button>`;

    document.getElementById(containerId)?.appendChild(block);

    const ta = block.querySelector('textarea');
    bindScriptTextareaAutosize(ta);
};

// 감정 태그 삽입
document.addEventListener('change', (e) => {
    if (!e.target.classList.contains('emotion-insert-btn')) return;
    const textarea = e.target.closest('.script-block')?.querySelector('.script-text');
    if (e.target.value && textarea) {
        const tag = `[${e.target.value}]`;
        const start = textarea.selectionStart;
        textarea.value = textarea.value.substring(0, start) + tag + textarea.value.substring(textarea.selectionEnd);
        e.target.value = '';
        if (window.updateChatPreview) window.updateChatPreview();
        textarea.focus();
        textarea.selectionEnd = start + tag.length;
    }
});

// 분기 열기/닫기
window.addBranchBlock = () => {
    document.getElementById('branch-container')?.classList.remove('hidden');
    document.getElementById('btn-add-branch')?.classList.add('hidden');
};

window.removeBranchBlock = () => {
    document.getElementById('branch-container')?.classList.add('hidden');
    document.getElementById('btn-add-branch')?.classList.remove('hidden');
    if (window.updateChatPreview) window.updateChatPreview();
};

// 채팅 미리보기 업데이트
window.updateChatPreview = function () {
    const chatContainer = document.getElementById('preview-chat-container');
    if (!chatContainer) return;
    chatContainer.innerHTML = '';

    const nameA = document.getElementById('char-a-rep')?.value || '화자 A';
    const nameB = document.getElementById('char-b-rep')?.value || '화자 B';
    const charAImg = document.getElementById('char-a-img-bg')?.style.backgroundImage || '';
    const charBImg = document.getElementById('char-b-img-bg')?.style.backgroundImage || '';

    // 섹션별 대본 렌더링
    const renderBlocks = (containerSelector, sectionTitle = null) => {
        const blocks = document.querySelectorAll(`${containerSelector} .script-block`);
        if (blocks.length > 0 && sectionTitle) {
            chatContainer.innerHTML += `<div class="text-[9px] font-bold text-purple-500 bg-purple-50 py-1 px-3 rounded-full w-fit mx-auto my-4 border border-purple-100">${sectionTitle}</div>`;
        }

        blocks.forEach(block => {
            const type = block.querySelector('.speaker-select')?.value || block.dataset.speaker;
            const ta = block.querySelector('.script-text');
            if (!ta) return;

            // [태그]와 html 태그 제거
            let text = ta.value.replace(/\[.*?\]/g, '').replace(/<[^>]+>/g, '').trim();
            if (!text) return;
            text = text.replace(/\n/g, '<br>');

            if (type === 'NARRATOR') {
                chatContainer.innerHTML += `
                    <div class="my-4 px-4">
                        <div class="bg-[#F8F8F8] border border-[#EEEEEE] rounded-xl p-3 text-center">
                            <p class="text-[11px] text-gray-500 leading-relaxed font-medium">${text}</p>
                        </div>
                    </div>`;
            } else if (type === 'A') {
                chatContainer.innerHTML += `
                    <div class="flex items-start gap-2 mb-4 px-2">
                        <div class="w-8 h-8 bg-gray-200 rounded-full flex-none bg-cover bg-center border border-gray-100" style="${charAImg ? `background-image:${charAImg}` : ''}"></div>
                        <div class="max-w-[80%]">
                            <p class="text-[10px] font-bold text-gray-400 mb-1 pl-1">${nameA}</p>
                            <div class="bg-white border border-[#EBEBEB] text-gray-800 text-[12px] px-3.5 py-3 rounded-2xl rounded-tl-none shadow-[0_2px_4px_rgba(0,0,0,0.02)] leading-relaxed">
                                ${text}
                            </div>
                        </div>
                    </div>`;
            } else if (type === 'B') {
                chatContainer.innerHTML += `
                    <div class="flex items-start gap-2 mb-4 flex-row-reverse px-2">
                        <div class="w-8 h-8 bg-[#E5E0D8] rounded-full flex-none bg-cover bg-center border border-[#DED6CC]" style="${charBImg ? `background-image:${charBImg}` : ''}"></div>
                        <div class="max-w-[80%] flex flex-col items-end">
                            <p class="text-[10px] font-bold text-gray-400 mb-1 pr-1">${nameB}</p>
                            <div class="bg-[#FDFBF9] border border-[#EBE2D5] text-[#5A4A35] text-[12px] px-3.5 py-3 rounded-2xl rounded-tr-none shadow-[0_2px_4px_rgba(0,0,0,0.02)] leading-relaxed">
                                ${text}
                            </div>
                        </div>
                    </div>`;
            }
        });
    };

    // 1. 시작 대본
    renderBlocks('#start-node-container');

    // 2. 분기 버튼 업데이트
    const branchContainer = document.getElementById('branch-container');
    const branchChoiceUI = document.getElementById('preview-branch-choice');

    if (branchContainer && !branchContainer.classList.contains('hidden')) {
        if (branchChoiceUI) branchChoiceUI.classList.remove('hidden');

        const labelA = document.getElementById('branch-a-label')?.value || 'A 선택지를 입력하세요';
        const labelB = document.getElementById('branch-b-label')?.value || 'B 선택지를 입력하세요';

        const btnA = document.getElementById('branch-btn-a');
        const btnB = document.getElementById('branch-btn-b');

        if (btnA) btnA.innerHTML = labelA.replace(/\n/g, '<br>');
        if (btnB) btnB.innerHTML = labelB.replace(/\n/g, '<br>');

        renderBlocks('#branch-a-node-container', 'OPTION A PATH');
        renderBlocks('#branch-b-node-container', 'OPTION B PATH');
    } else {
        if (branchChoiceUI) branchChoiceUI.classList.add('hidden');
    }

    // 3. 클로징 대본
    renderBlocks('#closing-node-container');

    chatContainer.scrollTop = chatContainer.scrollHeight;
};

// 분기 버튼 텍스트 입력 시 즉시 미리보기 반영
document.addEventListener('input', (e) => {
    if (e.target.id === 'branch-a-label' || e.target.id === 'branch-b-label') {
        if (window.updateChatPreview) window.updateChatPreview();
    }
});

// 분기 선택 (사용자 미리보기용)
window.selectBranch = function (branch) {
    const branchChoice = document.getElementById('preview-branch-choice');
    if (branchChoice) branchChoice.classList.add('hidden');

    const chatContainer = document.getElementById('preview-chat-container');
    if (!chatContainer) return;

    const nameA = document.getElementById('char-a-rep')?.value || '화자 A';
    const nameB = document.getElementById('char-b-rep')?.value || '화자 B';
    const charAImg = document.getElementById('char-a-img-bg')?.style.backgroundImage || '';
    const charBImg = document.getElementById('char-b-img-bg')?.style.backgroundImage || '';

    // 선택 배너
    const choiceLabel = branch === 'A'
        ? (document.getElementById('branch-a-label')?.value || 'A 선택')
        : (document.getElementById('branch-b-label')?.value || 'B 선택');
    chatContainer.innerHTML += `<div class="flex justify-center my-2"><span class="text-[10px] bg-[#7C4A3A]/10 text-[#7C4A3A] font-bold px-3 py-1 rounded-full">"${choiceLabel}" 선택</span></div>`;

    // 선택된 분기 대본
    const nodeId = branch === 'A' ? 'branch-a-node-container' : 'branch-b-node-container';
    document.querySelectorAll(`#${nodeId} .script-block`).forEach(block => {
        const type = block.querySelector('.speaker-select')?.value || block.dataset.speaker;
        const ta = block.querySelector('.script-text');
        if (!ta) return;
        let text = ta.value.replace(/<[^>]+>/g, '').trim();
        if (!text) return;
        text = text.replace(/\n/g, '<br>');
        chatContainer.innerHTML += _buildBubble(type, text, nameA, nameB, charAImg, charBImg);
    });

    // 클로징 대본
    document.querySelectorAll('#closing-node-container .script-block').forEach(block => {
        const type = block.querySelector('.speaker-select')?.value || block.dataset.speaker;
        const ta = block.querySelector('.script-text');
        if (!ta) return;
        let text = ta.value.replace(/<[^>]+>/g, '').trim();
        if (!text) return;
        text = text.replace(/\n/g, '<br>');
        chatContainer.innerHTML += _buildBubble(type, text, nameA, nameB, charAImg, charBImg);
    });

    chatContainer.scrollTop = chatContainer.scrollHeight;
};

// 말풍선 HTML 생성
function _buildBubble(type, text, nameA, nameB, charAImg, charBImg) {
    if (type === 'NARRATOR') {
        return `<p class="text-[11px] text-center text-gray-400 my-5 bg-gray-50 py-2 rounded-full w-[90%] mx-auto font-medium">${text}</p>`;
    }
    if (type === 'A') {
        return `
        <div class="flex items-start gap-2 mb-5">
            <div class="w-8 h-8 bg-gray-200 rounded-full flex-none bg-cover bg-center border border-gray-100" style="${charAImg ? 'background-image:' + charAImg : ''}"></div>
            <div class="max-w-[75%]">
                <p class="text-[10px] font-black text-gray-700 mb-1.5 pl-1">${nameA}</p>
                <div class="bg-white border border-gray-200 text-gray-800 text-[12px] px-3.5 py-3 rounded-2xl rounded-tl-none shadow-sm leading-relaxed">${text}</div>
            </div>
        </div>`;
    }
    if (type === 'B') {
        return `
        <div class="flex items-start gap-2 mb-5 flex-row-reverse">
            <div class="w-8 h-8 bg-[#E5E0D8] rounded-full flex-none bg-cover bg-center border border-[#E5E0D8]" style="${charBImg ? 'background-image:' + charBImg : ''}"></div>
            <div class="max-w-[75%] flex flex-col items-end">
                <p class="text-[10px] font-black text-gray-700 mb-1.5 pr-1">${nameB}</p>
                <div class="bg-[#FDF9F4] border border-[#EBE2D5] text-[#5A4A35] text-[12px] px-3.5 py-3 rounded-2xl rounded-tr-none shadow-sm leading-relaxed">${text}</div>
            </div>
        </div>`;
    }
    return '';
}

// 오디오 플레이어
function _getAudio() {
    return document.getElementById('preview-audio');
}

function _formatTime(secs) {
    if (isNaN(secs) || !isFinite(secs)) return '0:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
}

function _updatePlayIcon() {
    const a = _getAudio();
    const icon = document.getElementById('audio-play-icon');
    if (!icon) return;
    icon.setAttribute('d', (a && !a.paused) ? 'M6 19h4V5H6v14zm8-14v14h4V5h-4z' : 'M8 5v14l11-7z');
}

window.toggleAudio = function () {
    const a = _getAudio();
    if (!a) return;
    if (!a.src || a.src === window.location.href) {
        _openAudioPicker();
        return;
    }
    a.paused ? a.play() : a.pause();
};

window.seekRelative = function (seconds) {
    const a = _getAudio();
    if (!a || !a.duration) return;
    a.currentTime = Math.max(0, Math.min(a.duration, a.currentTime + seconds));
};

window.seekAudio = function (event) {
    const a = _getAudio();
    const bar = document.getElementById('audio-progress-bar');
    if (!a || !a.duration || !bar) return;
    const rect = bar.getBoundingClientRect();
    a.currentTime = ((event.clientX - rect.left) / rect.width) * a.duration;
};

function _openAudioPicker() {
    let picker = document.getElementById('audio-file-picker');
    if (!picker) {
        picker = document.createElement('input');
        picker.type = 'file';
        picker.accept = 'audio/*';
        picker.id = 'audio-file-picker';
        picker.style.display = 'none';
        document.body.appendChild(picker);
        picker.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const a = _getAudio();
            if (!a) return;
            a.src = URL.createObjectURL(file);
            a.load();
            a.play();
        });
    }
    picker.click();
}

document.addEventListener('DOMContentLoaded', () => {
    initScriptTextareaAutosize();

    const a = _getAudio();
    if (!a) return;

    a.addEventListener('timeupdate', () => {
        const pct = a.duration ? (a.currentTime / a.duration * 100) : 0;
        const fill = document.getElementById('audio-progress-fill');
        const thumb = document.getElementById('audio-progress-thumb');
        const cur = document.getElementById('audio-current-time');
        if (fill) fill.style.width = pct + '%';
        if (thumb) thumb.style.left = pct + '%';
        if (cur) cur.textContent = _formatTime(a.currentTime);
    });

    a.addEventListener('durationchange', () => {
        const total = document.getElementById('audio-total-time');
        if (total) total.textContent = _formatTime(a.duration);
    });

    a.addEventListener('play', _updatePlayIcon);
    a.addEventListener('pause', _updatePlayIcon);
    a.addEventListener('ended', _updatePlayIcon);

    // 드래그 앤 드롭으로 오디오 파일 로드
    const playerArea = document.querySelector('#preview-battle-chat .h-\\[100px\\]');
    if (playerArea) {
        playerArea.addEventListener('dragover', e => e.preventDefault());
        playerArea.addEventListener('drop', e => {
            e.preventDefault();
            const file = e.dataTransfer.files[0];
            if (!file || !file.type.startsWith('audio/')) return;
            a.src = URL.createObjectURL(file);
            a.load();
            a.play();
        });
    }
});

// 미리보기 화면 전환
window.switchToChatView = function () {
    document.getElementById('preview-battle-intro')?.classList.add('hidden');
    document.getElementById('preview-battle-chat')?.classList.remove('hidden');
    if (window.updateChatPreview) window.updateChatPreview();
};

window.switchToIntroView = function () {
    document.getElementById('preview-battle-chat')?.classList.add('hidden');
    document.getElementById('preview-battle-intro')?.classList.remove('hidden');
    const a = _getAudio();
    if (a) {
        a.pause();
        a.currentTime = 0;
        _updatePlayIcon();
    }
};

window.resetChatPreview = function () {
    if (window.updateChatPreview) window.updateChatPreview();
    const a = _getAudio();
    if (a) {
        a.pause();
        a.currentTime = 0;
        _updatePlayIcon();
    }
};
