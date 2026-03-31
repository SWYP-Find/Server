// action 파라미터: 'PENDING'(임시저장), 'EDIT'(단순수정), 'PUBLISH'(발행 및 오디오생성)
window.saveContent = async (action) => {
    const targetStatus = action === 'PENDING' ? 'PENDING' : 'PUBLISHED';

    const loader = document.getElementById('global-loader');
    const loaderText = document.getElementById('loader-text');
    if (loader) {
        if (loaderText) {
            if (action === 'PUBLISH') loaderText.innerText = "발행 및 오디오 생성 중...";
            else if (action === 'EDIT') loaderText.innerText = "수정된 내용 저장 중...";
            else loaderText.innerText = "임시저장 중...";
        }
        loader.classList.remove('hidden');
        loader.classList.add('flex');
    }

    try {
        let thumbnailUrl = PickeData.existingUrls.thumbnail;
        let charAUrl = PickeData.existingUrls.charA;
        let charBUrl = PickeData.existingUrls.charB;

        try {
            if (PickeData.uploadedFiles.thumbnail) thumbnailUrl = await window.uploadImageToServer(PickeData.uploadedFiles.thumbnail, 'BATTLE');
            if (PickeData.uploadedFiles.charA) charAUrl = await window.uploadImageToServer(PickeData.uploadedFiles.charA, 'PHILOSOPHER');
            if (PickeData.uploadedFiles.charB) charBUrl = await window.uploadImageToServer(PickeData.uploadedFiles.charB, 'PHILOSOPHER');
        } catch (e) { throw new Error("이미지 업로드에 실패했습니다."); }

        if (thumbnailUrl && typeof thumbnailUrl === 'object') {
            thumbnailUrl = thumbnailUrl.s3Key || thumbnailUrl.presignedUrl || String(thumbnailUrl);
        }
        if (charAUrl && typeof charAUrl === 'object') {
            charAUrl = charAUrl.s3Key || charAUrl.presignedUrl || String(charAUrl);
        }
        if (charBUrl && typeof charBUrl === 'object') {
            charBUrl = charBUrl.s3Key || charBUrl.presignedUrl || String(charBUrl);
        }

        // 1. 페이로드 구조
        let payload = {
            status: targetStatus,
            type: PickeData.currentContentType,
            tagIds: PickeData.selections.BASIC,
            thumbnailUrl: thumbnailUrl,
            targetDate: new Date().toISOString().split('T')[0],
            title: '',
            titlePrefix: null,
            titleSuffix: null,
            itemA: null,
            itemADesc: null,
            itemB: null,
            itemBDesc: null,
            summary: '',
            description: '',
            options: []
        };

        // 2. 타입별 데이터 매핑
        // [BATTLE]
        if (PickeData.currentContentType === 'BATTLE') {
            payload.title = document.getElementById('content-title')?.value || '';
            payload.summary = document.getElementById('content-summary')?.value || '';
            payload.description = document.getElementById('content-desc')?.value || '';
            payload.options = [
                { label: 'A', title: document.getElementById('char-a-title')?.value || '', stance: document.getElementById('char-a-stance')?.value || '', representative: document.getElementById('char-a-rep')?.value || '', /*quote: document.getElementById('char-a-quote')?.value || '',*/ imageUrl: charAUrl, tagIds: PickeData.selections.A },
                { label: 'B', title: document.getElementById('char-b-title')?.value || '', stance: document.getElementById('char-b-stance')?.value || '', representative: document.getElementById('char-b-rep')?.value || '', quote: document.getElementById('char-b-quote')?.value || '', imageUrl: charBUrl, tagIds: PickeData.selections.B }
            ];
        }
        // [QUIZ]
        else if (PickeData.currentContentType === 'QUIZ') {
            payload.title = document.getElementById('quiz-question')?.value || '';
            payload.description = document.getElementById('quiz-desc')?.value || '';
            payload.itemA = document.getElementById('quiz-perf-a')?.value || '';
            payload.itemADesc = document.getElementById('quiz-detail-a')?.value || '';
            payload.itemB = document.getElementById('quiz-perf-b')?.value || '';
            payload.itemBDesc = document.getElementById('quiz-detail-b')?.value || '';
            payload.options = [
                {
                    label: 'A',
                    title: document.getElementById('quiz-o-text')?.value || '',
                    stance: document.getElementById('quiz-o-desc')?.value || '',
                    imageUrl: null,
                    tagIds: []
                },
                {
                    label: 'B',
                    title: document.getElementById('quiz-x-text')?.value || '',
                    stance: document.getElementById('quiz-x-desc')?.value || '',
                    imageUrl: null,
                    tagIds: []
                }
            ];
        }
        // [VOTE]
        else if (PickeData.currentContentType === 'VOTE') {
            payload.titlePrefix = document.getElementById('vote-q-prefix')?.value || '';
            payload.titleSuffix = document.getElementById('vote-q-suffix')?.value || '';
            payload.title = payload.titlePrefix;

            payload.description = document.getElementById('vote-desc')?.value || '';
            const voteOpts = [];
            for (let i = 1; i <= 4; i++) {
                const val = document.getElementById(`vote-opt-${i}`)?.value.trim();
                if (val) {
                    voteOpts.push({ label: String.fromCharCode(64 + i), title: val, stance: '', imageUrl: null, tagIds: [] });
                }
            }
            payload.options = voteOpts;
        }

        // 백엔드 DTO(String)에 맞게 모든 이미지 URL 객체를 문자열로 변환
        const extractUrlString = (urlObj) => {
            if (!urlObj) return null;
            if (typeof urlObj === 'string') return urlObj;
            return urlObj.s3Key || urlObj.presignedUrl || String(urlObj);
        };

        // 1. 썸네일 변환
        payload.thumbnailUrl = extractUrlString(payload.thumbnailUrl);

        // 2. 선택지(options) 안의 모든 이미지 변환
        if (payload.options && payload.options.length > 0) {
            payload.options.forEach(opt => {
                opt.imageUrl = extractUrlString(opt.imageUrl);
            });
        }

        // 3. 요청 및 시나리오 로직
        const battleUrl = PickeData.isEditMode ? PickeData.API.BATTLE_UPDATE(PickeData.currentContentId) : PickeData.API.BATTLE_CREATE;
        console.log("[STEP 1] 서버로 보내는 데이터(Payload):", payload);
        const battleRes = await fetch(battleUrl, {
            method: PickeData.isEditMode ? 'PATCH' : 'POST',
            headers: PickeData.getAuthHeaders(),
            body: JSON.stringify(payload)
        });
        if (!battleRes.ok) throw new Error("컨텐츠 정보 저장 실패");

        const battleData = await battleRes.json();
        const savedBattleId = PickeData.isEditMode ? PickeData.currentContentId : (
            battleData.result?.battleId || battleData.result?.id || battleData.data?.battleId || battleData.id
        );

        if (!PickeData.isEditMode) {
            PickeData.currentContentId = savedBattleId;
            PickeData.isEditMode = true;
        }

        // [SCENARIO]
        if (PickeData.currentContentType === 'BATTLE') {
            if (!savedBattleId) throw new Error("배틀 생성 후 ID를 가져오지 못했습니다.");
            const isInteractive = !document.getElementById('branch-container')?.classList.contains('hidden');
            const nodes = [];
            const extractScripts = (containerId) => {
                const scripts = [];
                document.querySelectorAll(`#${containerId} .script-block`).forEach(block => {
                    const speakerSelect = block.querySelector('.speaker-select');
                    const scriptTextArea = block.querySelector('.script-text');
                    if (speakerSelect && scriptTextArea) {
                        const speakerType = speakerSelect.value;
                        let speakerName = '나레이터';
                        if (speakerType === 'A') speakerName = document.getElementById('char-a-rep')?.value || '인물 A';
                        if (speakerType === 'B') speakerName = document.getElementById('char-b-rep')?.value || '인물 B';
                        scripts.push({ speakerType, speakerName, text: scriptTextArea.value });
                    }
                });
                return scripts;
            };
            const startOptions = [];
            if (isInteractive) {
                startOptions.push({ label: document.getElementById('branch-a-label')?.value || 'A', nextNodeName: 'BRANCH_A' });
                startOptions.push({ label: document.getElementById('branch-b-label')?.value || 'B', nextNodeName: 'BRANCH_B' });
            }
            nodes.push({ nodeName: 'START', isStartNode: true, autoNextNode: isInteractive ? null : 'CLOSING', scripts: extractScripts('start-node-container'), interactiveOptions: startOptions });
            if (isInteractive) {
                nodes.push({ nodeName: 'BRANCH_A', isStartNode: false, autoNextNode: 'CLOSING', scripts: extractScripts('branch-a-node-container'), interactiveOptions: [] });
                nodes.push({ nodeName: 'BRANCH_B', isStartNode: false, autoNextNode: 'CLOSING', scripts: extractScripts('branch-b-node-container'), interactiveOptions: [] });
            }
            nodes.push({ nodeName: 'CLOSING', isStartNode: false, autoNextNode: null, scripts: extractScripts('closing-node-container'), interactiveOptions: [] });
            const scenarioPayload = { battleId: savedBattleId, isInteractive, nodes, status: targetStatus };
            const scenMethod = PickeData.scenarioId ? 'PUT' : 'POST';
            const scenUrl = PickeData.scenarioId ? `/api/v1/admin/scenarios/${PickeData.scenarioId}` : `/api/v1/admin/scenarios`;
            const scenRes = await fetch(scenUrl, { method: scenMethod, headers: PickeData.getAuthHeaders(), body: JSON.stringify(scenarioPayload) });
            if (!scenRes.ok) throw new Error("시나리오 데이터 저장 실패");
            const scenData = await scenRes.json();
            if (!PickeData.scenarioId) PickeData.scenarioId = scenData.result?.scenarioId || scenData.result?.id || scenData.data?.scenarioId || scenData.data?.id || scenData.result || scenData.data || null;

            // 발행(PUBLISH) 버튼을 눌렀을 때만 오디오 생성
            if (action === 'PUBLISH' && PickeData.scenarioId) {
                await fetch(`/api/v1/admin/scenarios/${PickeData.scenarioId}`, {
                    method: 'PATCH', headers: PickeData.getAuthHeaders(), body: JSON.stringify({ status: 'PUBLISHED' })
                });
            }
        }

        if (loader) { loader.classList.add('hidden'); loader.classList.remove('flex'); }
        const modal = document.getElementById('custom-modal');
        if (modal) {
            document.getElementById('custom-modal-title').innerText = "저장 완료";

            let resultMsg = "";
            if (action === 'PENDING') resultMsg = "임시저장 되었습니다.";
            else if (action === 'EDIT') resultMsg = "수정 내용이 저장되었습니다.\n(오디오 갱신 안 됨)";
            else resultMsg = "발행 되었습니다!\n(새 오디오 생성 처리됨)";

            document.getElementById('custom-modal-message').innerText = resultMsg;

            modal.classList.remove('hidden');
            setTimeout(() => { modal.classList.add('opacity-100'); modal.classList.remove('opacity-0'); }, 10);
            document.getElementById('custom-modal-confirm').onclick = () => { window.location.href = "/api/v1/admin/picke/list"; };
        } else {
            window.location.href = "/api/v1/admin/picke/list";
        }
    } catch (e) {
        if (loader) { loader.classList.add('hidden'); loader.classList.remove('flex'); }
        console.error(e);
        alert(`저장 중 오류: ${e.message}`);
    }
};