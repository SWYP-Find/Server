document.addEventListener("DOMContentLoaded", () => {

    // 섹션 클릭 시 인트로 ↔ 채팅 화면 자동 전환
    document.addEventListener('focusin', (e) => {
        // 배틀 타입일 때만 작동
        if (PickeData.currentContentType !== 'BATTLE') return;

        const intro     = document.getElementById('preview-battle-intro');
        const chat      = document.getElementById('preview-battle-chat');
        const statusBar = document.getElementById('status-bar');

        if (!intro || !chat) return;

        // 1. 기본정보나 인물 정보 섹션을 클릭하면 -> 인트로 미리보기
        if (e.target.closest('#section-basic') || e.target.closest('#section-chars')) {
            intro.classList.remove('hidden');
            chat.classList.add('hidden');
            // 상태바 글자색 흰색으로 (배경이 어두우므로)
            if (statusBar) {
                statusBar.classList.remove('text-black');
                statusBar.classList.add('text-white');
            }
        }
        // 2. 대본(시나리오) 섹션을 클릭하면 -> 채팅 미리보기
        else if (e.target.closest('#section-script')) {
            intro.classList.add('hidden');
            chat.classList.remove('hidden');
            // 상태바 글자색 검정색으로 (배경이 밝으므로)
            if (statusBar) {
                statusBar.classList.remove('text-white');
                statusBar.classList.add('text-black');
            }
            // 채팅 내용 최신화
            if (window.updateChatPreview) window.updateChatPreview();
        }
    });

    // 상단 타입 토글 (배틀 / 퀴즈 / 투표 탭 전환)
    const toggleBtns = document.querySelectorAll('.type-toggle');
    toggleBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const target = e.currentTarget;
            const targetId = target.dataset.target;

            toggleBtns.forEach(b => {
                b.classList.remove('active', 'bg-white', 'text-black', 'shadow-sm');
                b.classList.add('text-gray-500');
            });
            target.classList.remove('text-gray-500');
            target.classList.add('active', 'bg-white', 'text-black', 'shadow-sm');

            document.querySelectorAll('.content-form').forEach(form => form.classList.add('hidden'));
            document.getElementById(targetId)?.classList.remove('hidden');

            document.querySelectorAll('[id^="preview-wrapper-"]').forEach(pw => pw.classList.add('hidden'));
            const typeKey = targetId.replace('form-', '');
            document.getElementById(`preview-wrapper-${typeKey}`)?.classList.remove('hidden');

            PickeData.currentContentType = typeKey.toUpperCase();
        });
    });

    // 배틀 미리보기 카드 선택 인터랙션
    const cardA  = document.getElementById('pv-battle-card-a');
    const cardB  = document.getElementById('pv-battle-card-b');

    const resetBattleSelection = () => {
        [cardA, cardB].forEach(c => {
            if (!c) return;
            c.classList.replace('border-[#7C4A3A]', 'border-[#E5E0D8]');
            c.classList.remove('bg-orange-50');
        });
    }

    cardA?.addEventListener('click', () => {
        resetBattleSelection();
        cardA.classList.replace('border-[#E5E0D8]', 'border-[#7C4A3A]');
        cardA.classList.add('bg-orange-50');
    });

    cardB?.addEventListener('click', () => {
        resetBattleSelection();
        cardB.classList.replace('border-[#E5E0D8]', 'border-[#7C4A3A]');
        cardB.classList.add('bg-orange-50');
    });

    // 시계 업데이트
    const _updateClock = () => {
        const el = document.getElementById('status-time');
        if (el) {
            const now = new Date();
            el.textContent = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`;
        }
    };
    _updateClock();
    setInterval(_updateClock, 30000);
});

// 전역 투표 옵션 선택 함수
window.selectVoteOption = function (btn) {
    document.querySelectorAll('.vote-option-btn').forEach(b => {
        b.classList.remove('border-[#D4B886]', 'bg-[#FDFBF7]');
        b.classList.add('border-[#EBE2D5]', 'bg-[#FAFAFA]');
    });
    btn.classList.add('border-[#D4B886]', 'bg-[#FDFBF7]');
    const optId = btn.getAttribute('data-opt-id');
    const optText = document.getElementById(`pv-vote-opt${optId}`)?.textContent || '';
    const blank = document.getElementById('pv-vote-blank');
    if (blank) blank.textContent = optText || '?';
};