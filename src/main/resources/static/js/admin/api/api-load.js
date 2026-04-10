window.loadContent = async function () {
    if (!PickeData.isEditMode) return;

    const type = PickeData.currentTypeParam === 'VOTE' ? 'POLL' : PickeData.currentTypeParam;
    const endpointByType = {
        BATTLE: PickeData.API.BATTLE_GET,
        QUIZ: PickeData.API.QUIZ_GET,
        POLL: PickeData.API.POLL_GET
    };

    const formTargetByType = {
        BATTLE: 'form-battle',
        QUIZ: 'form-quiz',
        POLL: 'form-vote'
    };

    const createEmptyScript = (speakerType = 'NARRATOR') => ({ speakerType, text: '' });

    const renderScenarioScripts = (containerId, scripts, defaultSpeaker = 'NARRATOR') => {
        const container = document.getElementById(containerId);
        if (!container) return;

        container.innerHTML = '';
        const safeScripts = Array.isArray(scripts) && scripts.length > 0
            ? scripts
            : [createEmptyScript(defaultSpeaker)];

        safeScripts.forEach((script) => {
            if (typeof window.addScriptBlock === 'function') {
                window.addScriptBlock(containerId, script.speakerType || defaultSpeaker);
            } else {
                return;
            }

            const blocks = container.querySelectorAll('.script-block');
            const block = blocks[blocks.length - 1];
            if (!block) return;

            const speakerSelect = block.querySelector('.speaker-select');
            if (speakerSelect) {
                speakerSelect.value = script.speakerType || defaultSpeaker;
            }

            const scriptText = block.querySelector('.script-text');
            if (scriptText) {
                scriptText.value = script.text || '';
                scriptText.dispatchEvent(new Event('input', { bubbles: true }));
            }
        });
    };

    const applyScenarioToForm = (scenario) => {
        if (!scenario) return;

        const nodes = Array.isArray(scenario.nodes) ? scenario.nodes : [];
        const nodeByName = {};
        const nodeNameById = {};

        nodes.forEach((node) => {
            if (!node) return;
            nodeByName[node.nodeName] = node;
            if (node.nodeId != null) {
                nodeNameById[node.nodeId] = node.nodeName;
            }
        });

        const startNode = nodeByName.START || null;
        const branchANode = nodeByName.BRANCH_A || null;
        const branchBNode = nodeByName.BRANCH_B || null;
        const closingNode = nodeByName.CLOSING || null;
        const isInteractive = !!scenario.isInteractive;

        const branchContainer = document.getElementById('branch-container');
        const addBranchButton = document.getElementById('btn-add-branch');
        if (branchContainer) {
            if (isInteractive) branchContainer.classList.remove('hidden');
            else branchContainer.classList.add('hidden');
        }
        if (addBranchButton) {
            if (isInteractive) addBranchButton.classList.add('hidden');
            else addBranchButton.classList.remove('hidden');
        }

        renderScenarioScripts('start-node-container', startNode?.scripts || [], 'NARRATOR');
        renderScenarioScripts('closing-node-container', closingNode?.scripts || [], 'NARRATOR');
        renderScenarioScripts('branch-a-node-container', branchANode?.scripts || [], 'A');
        renderScenarioScripts('branch-b-node-container', branchBNode?.scripts || [], 'B');

        const branchAInput = document.getElementById('branch-a-label');
        const branchBInput = document.getElementById('branch-b-label');

        const startOptions = Array.isArray(startNode?.interactiveOptions) ? startNode.interactiveOptions : [];
        let branchALabel = '';
        let branchBLabel = '';

        startOptions.forEach((option) => {
            const targetNodeName = option?.nextNodeId != null ? nodeNameById[option.nextNodeId] : null;
            if (targetNodeName === 'BRANCH_A') branchALabel = option.label || '';
            if (targetNodeName === 'BRANCH_B') branchBLabel = option.label || '';
        });

        if (branchAInput) branchAInput.value = branchALabel;
        if (branchBInput) branchBInput.value = branchBLabel;

        if (window.updateChatPreview) window.updateChatPreview();
    };

    try {
        const getter = endpointByType[type] || PickeData.API.BATTLE_GET;
        const res = await fetch(getter(PickeData.currentContentId), { headers: PickeData.getAuthHeaders() });
        if (!res.ok) return;

        const json = await res.json();
        const data = json.result || json.data || json;

        document.querySelector(`[data-target="${formTargetByType[type]}"]`)?.click();
        PickeData.currentContentType = type;

        Object.keys(PickeData.selections).forEach((key) => {
            PickeData.selections[key] = [];
        });

        PickeData.currentTargetDate = data.targetDate || PickeData.currentTargetDate;
        PickeData.currentStatus = data.status || 'PENDING';
        if (window.setTargetDateInputs) window.setTargetDateInputs(PickeData.currentTargetDate);

        if (type === 'BATTLE') {
            PickeData.setValue('content-title', data.title || '');
            PickeData.setValue('content-summary', data.summary || '');
            PickeData.setValue('content-desc', data.description || '');
            PickeData.setValue('battle-audio-duration', data.audioDuration ?? '');
            PickeData.setValue('battle-status', data.status || 'PENDING');
            PickeData.setValue('battle-thumbnail-url', data.thumbnailUrl || '');

            if (data.thumbnailUrl) {
                PickeData.existingUrls.thumbnail = data.thumbnailUrl;
                if (window.applyThumbnailPreview) window.applyThumbnailPreview(data.thumbnailUrl);
            }

            PickeData.selections.CATEGORY = (data.tags || []).map((tag) => tag.tagId || tag.id);

            const options = data.options || [];
            const optionA = options.find((option) => option.label === 'A');
            const optionB = options.find((option) => option.label === 'B');

            if (optionA) {
                PickeData.setValue('char-a-title', optionA.title || '');
                PickeData.setValue('char-a-stance', optionA.stance || '');
                PickeData.setValue('char-a-rep', optionA.representative || '');
                PickeData.setValue('char-a-display-order', optionA.displayOrder ?? 1);
                PickeData.setValue('char-a-image-url', optionA.imageUrl || '');
                if (optionA.imageUrl) {
                    PickeData.existingUrls.charA = optionA.imageUrl;
                    PickeData.setPreviewImage('char-a-img-bg', 'char-a-img-placeholder', 'intro-char-a-img', optionA.imageUrl);
                }
                PickeData.selections.BATTLE_A_PHILOSOPHER = (optionA.tags || [])
                    .filter((tag) => tag.type === 'PHILOSOPHER')
                    .map((tag) => tag.tagId || tag.id);
                PickeData.selections.BATTLE_A_VALUE = (optionA.tags || [])
                    .filter((tag) => tag.type === 'VALUE')
                    .map((tag) => tag.tagId || tag.id);
            }

            if (optionB) {
                PickeData.setValue('char-b-title', optionB.title || '');
                PickeData.setValue('char-b-stance', optionB.stance || '');
                PickeData.setValue('char-b-rep', optionB.representative || '');
                PickeData.setValue('char-b-display-order', optionB.displayOrder ?? 2);
                PickeData.setValue('char-b-image-url', optionB.imageUrl || '');
                if (optionB.imageUrl) {
                    PickeData.existingUrls.charB = optionB.imageUrl;
                    PickeData.setPreviewImage('char-b-img-bg', 'char-b-img-placeholder', 'intro-char-b-img', optionB.imageUrl);
                }
                PickeData.selections.BATTLE_B_PHILOSOPHER = (optionB.tags || [])
                    .filter((tag) => tag.type === 'PHILOSOPHER')
                    .map((tag) => tag.tagId || tag.id);
                PickeData.selections.BATTLE_B_VALUE = (optionB.tags || [])
                    .filter((tag) => tag.type === 'VALUE')
                    .map((tag) => tag.tagId || tag.id);
            }

            try {
                const scenRes = await fetch(`/api/v1/admin/battles/${PickeData.currentContentId}/scenario`, { headers: PickeData.getAuthHeaders() });
                if (scenRes.ok) {
                    const scenJson = await scenRes.json();
                    const scenario = scenJson.result || scenJson.data;
                    if (scenario) {
                        const voiceSettings = scenario.voiceSettings || {};
                        PickeData.setValue('tts-voice-narrator', voiceSettings.NARRATOR || '');
                        PickeData.setValue('tts-voice-a', voiceSettings.A || '');
                        PickeData.setValue('tts-voice-b', voiceSettings.B || '');
                        PickeData.setValue('tts-voice-user', voiceSettings.USER || '');
                        PickeData.scenarioId = scenario.scenarioId || scenario.id || null;
                        applyScenarioToForm(scenario);
                    }
                }
            } catch (e) {
                console.error('시나리오를 불러오지 못했습니다:', e);
            }
        }

        if (type === 'QUIZ') {
            PickeData.setValue('quiz-title', data.title || '');
            PickeData.setValue('quiz-status', data.status || 'PENDING');

            const options = data.options || [];
            const optionA = options.find((option) => option.label === 'A');
            const optionB = options.find((option) => option.label === 'B');

            if (optionA) {
                PickeData.setValue('quiz-option-a-title', optionA.text || '');
                PickeData.setValue('quiz-option-a-detail', optionA.detailText || '');
                PickeData.setValue('quiz-option-a-display-order', optionA.displayOrder ?? 1);
                if (optionA.isCorrect) document.getElementById('quiz-answer-a').checked = true;
            }

            if (optionB) {
                PickeData.setValue('quiz-option-b-title', optionB.text || '');
                PickeData.setValue('quiz-option-b-detail', optionB.detailText || '');
                PickeData.setValue('quiz-option-b-display-order', optionB.displayOrder ?? 2);
                if (optionB.isCorrect) document.getElementById('quiz-answer-b').checked = true;
            }
        }

        if (type === 'POLL') {
            PickeData.setValue('poll-title-prefix', data.titlePrefix || '');
            PickeData.setValue('poll-title-suffix', data.titleSuffix || '');
            PickeData.setValue('poll-status', data.status || 'PENDING');

            const optionTargetByLabel = {
                A: { titleId: 'poll-option-1-title', orderId: 'poll-option-1-display-order' },
                B: { titleId: 'poll-option-2-title', orderId: 'poll-option-2-display-order' },
                C: { titleId: 'poll-option-3-title', orderId: 'poll-option-3-display-order' },
                D: { titleId: 'poll-option-4-title', orderId: 'poll-option-4-display-order' }
            };

            (data.options || []).forEach((option) => {
                const mapping = optionTargetByLabel[option.label];
                if (!mapping) return;
                PickeData.setValue(mapping.titleId, option.title || '');
                PickeData.setValue(mapping.orderId, option.displayOrder ?? null);
            });
        }

        if (window.refreshFormBadges) window.refreshFormBadges();
        if (window.updatePreviewTags) window.updatePreviewTags();

        document.querySelectorAll('textarea, input').forEach((el) => {
            el.dispatchEvent(new Event('input', { bubbles: true }));
        });

        if (window.updateChatPreview) window.updateChatPreview();
        if (window.updateButtonStates) window.updateButtonStates(data.status);
    } catch (e) {
        console.error('콘텐츠를 불러오는 중 오류가 발생했습니다:', e);
    }
};

window.updateButtonStates = function (currentStatus) {
    const btnPending = document.getElementById('btn-save-pending');
    const btnPublish = document.getElementById('btn-save-publish');
    const btnRepublish = document.getElementById('btn-republish-audio');

    if (PickeData.isEditMode && btnPublish) {
        if (currentStatus === 'PUBLISHED') {
            btnPublish.innerText = '수정 저장 (텍스트만)';
            btnPublish.onclick = () => window.saveContent('EDIT');
        } else {
            btnPublish.innerText = '발행';
            btnPublish.onclick = () => window.saveContent('PUBLISHED');
        }
    }

    if (currentStatus === 'PUBLISHED') {
        if (btnPending) {
            btnPending.disabled = true;
            btnPending.classList.add('opacity-50', 'cursor-not-allowed', 'bg-gray-200', 'text-gray-400');
        }
        if (btnRepublish && PickeData.currentContentType === 'BATTLE') {
            btnRepublish.classList.remove('hidden');
        }
    } else {
        if (btnRepublish) btnRepublish.classList.add('hidden');
        if (btnPending) {
            btnPending.disabled = false;
            btnPending.classList.remove('opacity-50', 'cursor-not-allowed', 'bg-gray-200', 'text-gray-400');
        }
    }
};

window.confirmRepublish = function () {
    const isConfirmed = confirm('시나리오 오디오를 다시 생성할까요? TTS 사용량이 증가할 수 있습니다.');
    if (isConfirmed) {
        window.saveContent('PUBLISH');
    }
};
