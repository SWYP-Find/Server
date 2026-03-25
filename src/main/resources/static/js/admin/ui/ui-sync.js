document.addEventListener('input', (e) => {
    const id  = e.target.id;
    const val = e.target.value;
    const fv  = val.replace(/\n/g, '<br>');

    // Textarea 자동 높이 조절
    if (e.target.tagName.toLowerCase() === 'textarea') {
        e.target.style.height = 'auto';
        e.target.style.height = (e.target.scrollHeight) + 'px';
    }

    const set = (elId, content, html = false) => {
        const el = document.getElementById(elId);
        if (el) html ? el.innerHTML = content : el.innerText = content;
    };

    // 배틀 실시간 동기화
    if (id === 'content-title') {
        set('preview-title-intro', fv || '제목을 입력해주세요', true);
        set('preview-title-chat', val || '제목 없음');
    }
    if (id === 'content-summary') set('preview-desc', fv || '설명이 여기에 표시됩니다.', true);
    if (id === 'char-a-title')    set('preview-char-a-name', val || 'A 이름');
    if (id === 'char-a-stance')   set('preview-char-a-stance', fv, true);
    if (id === 'char-b-title')    set('preview-char-b-name', val || 'B 이름');
    if (id === 'char-b-stance')   set('preview-char-b-stance', fv, true);

    // 퀴즈 실시간 동기화
    if (id === 'quiz-question') set('pv-quiz-q', fv || '퀴즈 질문을 입력하세요.', true);
    if (id === 'quiz-o-text')   set('pv-quiz-o-text', val || '텍스트');
    if (id === 'quiz-o-desc')   set('pv-quiz-o-desc', val || '설명');
    if (id === 'quiz-x-text')   set('pv-quiz-x-text', val || '텍스트');
    if (id === 'quiz-x-desc')   set('pv-quiz-x-desc', val || '설명');
    if (id === 'quiz-desc')     set('pv-quiz-desc', fv, true);

    // 투표 실시간 동기화
    if (id === 'vote-q-prefix') set('pv-vote-prefix', val || '앞 내용');
    if (id === 'vote-q-suffix') set('pv-vote-suffix', val || '뒷 내용');
    if (id === 'vote-desc')     set('pv-vote-desc', fv, true);
    if (id.startsWith('vote-opt-')) {
        const num = id.split('-').pop();
        set(`pv-vote-opt${num}`, val || `보기${num}`);
    }

    if (id === 'branch-a-label' || id === 'branch-b-label') {
        if (window.updateChatPreview) window.updateChatPreview();
    }
});