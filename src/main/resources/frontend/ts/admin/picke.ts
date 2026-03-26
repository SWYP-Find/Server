type ContentType = 'battle' | 'quiz' | 'vote';

interface BaseContent {
    type: ContentType;
    title: string;
    category?: string[];
    hint?: string;
}

interface BattleContent extends BaseContent {
    characterA: { name: string; position: string; desc: string };
    characterB: { name: string; position: string; desc: string };
    scripts: any[];
}

interface QuizContent extends BaseContent {
    question: string;
    answerO: string;
    answerX: string;
}

document.addEventListener("DOMContentLoaded", () => {
    let currentTab: ContentType = 'battle';

    const tabButtons = document.querySelectorAll<HTMLButtonElement>('#tab-container button');
    const contentTitleInput = document.getElementById('content-title') as HTMLInputElement | null;
    const previewTitle = document.getElementById('preview-title') as HTMLElement | null;
    const submitBtn = document.getElementById('btn-submit') as HTMLButtonElement | null;

    tabButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            const tabName = this.dataset.tab as ContentType;
            if (tabName) switchTab(tabName);
        });
    });

    if (contentTitleInput) {
        contentTitleInput.addEventListener('input', (e: Event) => {
            const target = e.target as HTMLInputElement;
            if (previewTitle) previewTitle.innerHTML = target.value || '제목 없음';
        });
    }

    if (submitBtn) {
        submitBtn.addEventListener('click', submitContent);
    }

    function switchTab(tabName: ContentType): void {
        currentTab = tabName;

        tabButtons.forEach(btn => {
            btn.className = btn.dataset.tab === tabName
                ? "px-6 py-2 rounded-full text-sm font-bold bg-black text-white transition-all"
                : "px-6 py-2 rounded-full text-sm font-bold text-gray-500 hover:text-black transition-all";
        });

        ['battle', 'quiz', 'vote'].forEach(t => {
            const form = document.getElementById(`form-${t}`) as HTMLElement | null;
            if (form) {
                form.classList.toggle('hidden', t !== tabName);
                form.classList.toggle('block', t === tabName);
            }
        });

        updatePreview(tabName);
    }

    function updatePreview(tabName: ContentType): void {
        const badge = document.getElementById('preview-badge') as HTMLElement | null;
        const battleIntro = document.getElementById('preview-battle-intro') as HTMLElement | null;
        const quizView = document.getElementById('preview-quiz-view') as HTMLElement | null;

        if (battleIntro) battleIntro.classList.add('hidden');
        if (quizView) quizView.classList.add('hidden');

        if (badge) {
            badge.innerText = `${tabName.toUpperCase()} MODE`;
            const color = tabName === 'battle' ? 'purple' : tabName === 'quiz' ? 'blue' : 'green';
            badge.className = `px-2 py-1 bg-${color}-100 text-${color}-700 text-[10px] font-bold rounded`;
        }

        if (tabName === 'battle' && battleIntro) battleIntro.classList.remove('hidden');
        if (tabName === 'quiz' && quizView) quizView.classList.remove('hidden');
    }

    function submitContent(): void {
        const title = contentTitleInput?.value || '';
        if (!title.trim()) {
            alert("제목을 입력해주세요.");
            contentTitleInput?.focus();
            return;
        }

        // 현재 탭에 따른 데이터 수집
        let payload: any = { type: currentTab, title: title };
        // TODO: 여기서 폼의 각 input 값을 payload에 담아줍니다.

        console.log("서버로 전송할 데이터:", payload);
        alert(`[${currentTab.toUpperCase()}] 콘텐츠가 콘솔에 출력되었습니다. API 연동을 진행해주세요.`);
    }
});