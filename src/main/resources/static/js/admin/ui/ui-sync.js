document.addEventListener('input', (e) => {
    const id = e.target.id;
    const val = e.target.value;
    const fv = val.replace(/\n/g, '<br>');

    if (e.target.tagName.toLowerCase() === 'textarea') {
        e.target.style.height = 'auto';
        e.target.style.height = `${e.target.scrollHeight}px`;
    }

    const set = (elId, content, html = false) => {
        const el = document.getElementById(elId);
        if (!el) return;
        if (html) el.innerHTML = content;
        else el.innerText = content;
    };

    if (id === 'content-title') {
        set('preview-title-intro', fv || '제목', true);
        set('preview-title-chat', val || '설명');
    }
    if (id === 'content-desc') set('preview-desc', fv || '설명', true);

    if (id === 'char-a-title') set('preview-char-a-title', val || '주장');
    if (id === 'char-a-rep') set('preview-char-a-rep', val || '철학자');
    if (id === 'char-a-stance') set('preview-char-a-stance', fv, true);

    if (id === 'char-b-title') set('preview-char-b-title', val || '주장');
    if (id === 'char-b-rep') set('preview-char-b-rep', val || '철학자');
    if (id === 'char-b-stance') set('preview-char-b-stance', fv, true);

    if (id === 'quiz-title') set('pv-quiz-q', fv || '퀴즈 제목', true);
    if (id === 'quiz-option-a-title') set('pv-quiz-o-text', val || '참여문학');
    if (id === 'quiz-option-a-detail') set('pv-quiz-o-desc', val || '참여문학은 좋습니다.');
    if (id === 'quiz-option-b-title') set('pv-quiz-x-text', val || '순수문학');
    if (id === 'quiz-option-b-detail') set('pv-quiz-x-desc', val || '순수문학은 좋습니다.');

    if (id === 'poll-title-prefix') set('pv-vote-prefix', val || '나에게 예술이란');
    if (id === 'poll-title-suffix') set('pv-vote-suffix', val || ' 하는 행위이다.');
    if (id.startsWith('poll-option-') && id.endsWith('-title')) {
        const num = id.split('-')[2];
        set(`pv-vote-opt${num}`, val || `Option ${num}`);
        set(`pv-bar-label-${num}`, val || `Option ${num}`);
    }

    if (id === 'branch-a-label' || id === 'branch-b-label' || id === 'char-a-rep' || id === 'char-b-rep') {
        if (window.updateChatPreview) window.updateChatPreview();
    }
});