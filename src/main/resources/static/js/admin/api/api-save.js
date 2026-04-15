window.saveContent = async (action) => {
    const loader = document.getElementById('global-loader');
    const loaderText = document.getElementById('loader-text');

    const statusFromAction = action === 'PENDING' ? 'PENDING' : 'PUBLISHED';

    if (loader) {
        if (loaderText) {
            if (action === 'PUBLISHED' || action === 'PUBLISH') loaderText.innerText = '콘텐츠를 발행하는 중입니다...';
            else if (action === 'EDIT') loaderText.innerText = '콘텐츠를 수정하는 중입니다...';
            else loaderText.innerText = '임시 저장 중입니다...';
        }
        loader.classList.remove('hidden');
        loader.classList.add('flex');
    }

    const toUrlString = (urlObj) => {
        if (!urlObj) return null;
        if (typeof urlObj === 'string') return urlObj;
        return urlObj.s3Key || urlObj.presignedUrl || String(urlObj);
    };

    const asIntOrNull = (value) => {
        if (value == null || value === '') return null;
        const parsed = Number(value);
        return Number.isNaN(parsed) ? null : parsed;
    };

    const setHiddenImageValue = (id, value) => {
        const input = document.getElementById(id);
        if (input) input.value = value || '';
    };

    const getTargetDate = (type) => {
        const inputIdByType = {
            BATTLE: 'battle-target-date',
            QUIZ: 'quiz-target-date',
            POLL: 'poll-target-date'
        };
        return document.getElementById(inputIdByType[type])?.value || PickeData.currentTargetDate || new Date().toISOString().split('T')[0];
    };

    const getStatus = (type) => {
        if (action === 'PENDING') return 'PENDING';
        if (action === 'PUBLISHED' || action === 'PUBLISH') return 'PUBLISHED';

        const statusInputByType = {
            BATTLE: 'battle-status',
            QUIZ: 'quiz-status',
            POLL: 'poll-status'
        };
        return document.getElementById(statusInputByType[type])?.value || statusFromAction;
    };

    try {
        const currentType = PickeData.currentContentType;
        const previousStatus = PickeData.currentStatus;
        const targetDate = getTargetDate(currentType);
        const resolvedStatus = getStatus(currentType);
        const hasNewBattleImageUploads = currentType === 'BATTLE'
            && !!(PickeData.uploadedFiles.thumbnail || PickeData.uploadedFiles.charA || PickeData.uploadedFiles.charB);
        const shouldUploadAssets = action === 'PUBLISHED' || action === 'PUBLISH' || (action === 'EDIT' && hasNewBattleImageUploads);
        const shouldUploadLocalDraft = action === 'PENDING';

        PickeData.currentTargetDate = targetDate;

        let thumbnailUrl = PickeData.existingUrls.thumbnail;
        let charAUrl = PickeData.existingUrls.charA;
        let charBUrl = PickeData.existingUrls.charB;

        if (currentType === 'BATTLE') {
            const uploadByAction = async (file, category) => {
                if (!file) return null;
                if (shouldUploadAssets) return window.uploadImageToServer(file, category);
                if (shouldUploadLocalDraft) return window.uploadImageToLocalDraft(file);
                return null;
            };

            if (PickeData.uploadedFiles.thumbnail) {
                thumbnailUrl = await uploadByAction(PickeData.uploadedFiles.thumbnail, 'BATTLE');
            }
            if (PickeData.uploadedFiles.charA) {
                charAUrl = await uploadByAction(PickeData.uploadedFiles.charA, 'PHILOSOPHER');
            }
            if (PickeData.uploadedFiles.charB) {
                charBUrl = await uploadByAction(PickeData.uploadedFiles.charB, 'PHILOSOPHER');
            }
        }

        thumbnailUrl = toUrlString(thumbnailUrl);
        charAUrl = toUrlString(charAUrl);
        charBUrl = toUrlString(charBUrl);

        PickeData.existingUrls.thumbnail = thumbnailUrl || null;
        PickeData.existingUrls.charA = charAUrl || null;
        PickeData.existingUrls.charB = charBUrl || null;

        setHiddenImageValue('battle-thumbnail-url', thumbnailUrl);
        setHiddenImageValue('char-a-image-url', charAUrl);
        setHiddenImageValue('char-b-image-url', charBUrl);

        let payload = null;
        let requestUrl = '';

        if (currentType === 'BATTLE') {
            payload = {
                status: resolvedStatus,
                title: document.getElementById('content-title')?.value || '',
                summary: document.getElementById('content-summary')?.value || '',
                description: document.getElementById('content-desc')?.value || '',
                thumbnailUrl: thumbnailUrl || document.getElementById('battle-thumbnail-url')?.value || null,
                targetDate,
                audioDuration: asIntOrNull(document.getElementById('battle-audio-duration')?.value),
                tagIds: PickeData.selections.CATEGORY || [],
                options: [
                    {
                        label: 'A',
                        title: document.getElementById('char-a-title')?.value || '',
                        stance: document.getElementById('char-a-stance')?.value || '',
                        representative: document.getElementById('char-a-rep')?.value || '',
                        imageUrl: charAUrl || document.getElementById('char-a-image-url')?.value || null,
                        displayOrder: asIntOrNull(document.getElementById('char-a-display-order')?.value) || 1,
                        tagIds: [
                            ...(PickeData.selections.BATTLE_A_PHILOSOPHER || []),
                            ...(PickeData.selections.BATTLE_A_VALUE || [])
                        ]
                    },
                    {
                        label: 'B',
                        title: document.getElementById('char-b-title')?.value || '',
                        stance: document.getElementById('char-b-stance')?.value || '',
                        representative: document.getElementById('char-b-rep')?.value || '',
                        imageUrl: charBUrl || document.getElementById('char-b-image-url')?.value || null,
                        displayOrder: asIntOrNull(document.getElementById('char-b-display-order')?.value) || 2,
                        tagIds: [
                            ...(PickeData.selections.BATTLE_B_PHILOSOPHER || []),
                            ...(PickeData.selections.BATTLE_B_VALUE || [])
                        ]
                    }
                ]
            };
            requestUrl = PickeData.isEditMode ? PickeData.API.BATTLE_UPDATE(PickeData.currentContentId) : PickeData.API.BATTLE_CREATE;
        } else if (currentType === 'QUIZ') {
            payload = {
                title: document.getElementById('quiz-title')?.value || '',
                targetDate,
                status: resolvedStatus,
                options: [
                    {
                        label: 'A',
                        text: document.getElementById('quiz-option-a-title')?.value || '',
                        detailText: document.getElementById('quiz-option-a-detail')?.value || '',
                        isCorrect: document.getElementById('quiz-answer-a')?.checked || false,
                        displayOrder: asIntOrNull(document.getElementById('quiz-option-a-display-order')?.value) || 1
                    },
                    {
                        label: 'B',
                        text: document.getElementById('quiz-option-b-title')?.value || '',
                        detailText: document.getElementById('quiz-option-b-detail')?.value || '',
                        isCorrect: document.getElementById('quiz-answer-b')?.checked || false,
                        displayOrder: asIntOrNull(document.getElementById('quiz-option-b-display-order')?.value) || 2
                    }
                ]
            };
            requestUrl = PickeData.isEditMode ? PickeData.API.QUIZ_UPDATE(PickeData.currentContentId) : PickeData.API.QUIZ_CREATE;
        } else {
            const pollOptions = [
                { label: 'A', titleId: 'poll-option-1-title', orderId: 'poll-option-1-display-order' },
                { label: 'B', titleId: 'poll-option-2-title', orderId: 'poll-option-2-display-order' },
                { label: 'C', titleId: 'poll-option-3-title', orderId: 'poll-option-3-display-order' },
                { label: 'D', titleId: 'poll-option-4-title', orderId: 'poll-option-4-display-order' }
            ]
                .map((option, index) => ({
                    label: option.label,
                    title: document.getElementById(option.titleId)?.value || '',
                    displayOrder: asIntOrNull(document.getElementById(option.orderId)?.value) || (index + 1)
                }))
                .filter((option) => option.title.trim().length > 0);

            payload = {
                titlePrefix: document.getElementById('poll-title-prefix')?.value || '',
                titleSuffix: document.getElementById('poll-title-suffix')?.value || '',
                targetDate,
                status: resolvedStatus,
                options: pollOptions
            };
            requestUrl = PickeData.isEditMode ? PickeData.API.POLL_UPDATE(PickeData.currentContentId) : PickeData.API.POLL_CREATE;
        }

        const saveRes = await fetch(requestUrl, {
            method: PickeData.isEditMode ? 'PATCH' : 'POST',
            headers: PickeData.getAuthHeaders(),
            body: JSON.stringify(payload)
        });
        if (!saveRes.ok) throw new Error('콘텐츠 저장에 실패했습니다.');

        const saved = await saveRes.json();
        const result = saved.result || saved.data || {};
        const savedId = result.battleId || result.quizId || result.pollId || result.id || PickeData.currentContentId;

        if (!PickeData.isEditMode) {
            PickeData.currentContentId = savedId;
            PickeData.isEditMode = true;
        }

        if (currentType === 'BATTLE') {
            const isInteractive = !document.getElementById('branch-container')?.classList.contains('hidden');

            const extractScripts = (containerId) => {
                const scripts = [];
                document.querySelectorAll(`#${containerId} .script-block`).forEach((block) => {
                    const speakerSelect = block.querySelector('.speaker-select');
                    const scriptTextArea = block.querySelector('.script-text');
                    if (!speakerSelect || !scriptTextArea) return;

                    const speakerType = speakerSelect.value;
                    let speakerName = 'NARRATOR';
                    if (speakerType === 'A') speakerName = document.getElementById('char-a-rep')?.value || 'A';
                    if (speakerType === 'B') speakerName = document.getElementById('char-b-rep')?.value || 'B';
                    scripts.push({ speakerType, speakerName, text: scriptTextArea.value });
                });
                return scripts;
            };

            const startOptions = [];
            if (isInteractive) {
                startOptions.push({ label: document.getElementById('branch-a-label')?.value || 'A', nextNodeName: 'BRANCH_A' });
                startOptions.push({ label: document.getElementById('branch-b-label')?.value || 'B', nextNodeName: 'BRANCH_B' });
            }

            const nodes = [
                {
                    nodeName: 'START',
                    isStartNode: true,
                    autoNextNode: isInteractive ? null : 'CLOSING',
                    scripts: extractScripts('start-node-container'),
                    interactiveOptions: startOptions
                }
            ];

            if (isInteractive) {
                nodes.push({ nodeName: 'BRANCH_A', isStartNode: false, autoNextNode: 'CLOSING', scripts: extractScripts('branch-a-node-container'), interactiveOptions: [] });
                nodes.push({ nodeName: 'BRANCH_B', isStartNode: false, autoNextNode: 'CLOSING', scripts: extractScripts('branch-b-node-container'), interactiveOptions: [] });
            }

            nodes.push({ nodeName: 'CLOSING', isStartNode: false, autoNextNode: null, scripts: extractScripts('closing-node-container'), interactiveOptions: [] });

            const voiceSettings = {};
            const voiceInputMap = {
                NARRATOR: 'tts-voice-narrator',
                A: 'tts-voice-a',
                B: 'tts-voice-b',
                USER: 'tts-voice-user'
            };

            Object.entries(voiceInputMap).forEach(([speakerType, inputId]) => {
                const value = document.getElementById(inputId)?.value?.trim();
                if (value) voiceSettings[speakerType] = value;
            });

            if (action === 'PUBLISHED' || action === 'PUBLISH') {
                const requiredSpeakers = new Set();
                nodes.forEach((node) => {
                    (node.scripts || []).forEach((script) => {
                        if (script.speakerType) requiredSpeakers.add(script.speakerType);
                    });
                });

                const missingVoiceSpeakers = Array.from(requiredSpeakers).filter((speakerType) => !voiceSettings[speakerType]);
                if (missingVoiceSpeakers.length > 0) {
                    throw new Error(`다음 화자의 Fish Audio reference_id가 없습니다: ${missingVoiceSpeakers.join(', ')}`);
                }
            }

            const scenarioPayload = {
                battleId: savedId,
                isInteractive,
                nodes,
                status: resolvedStatus,
                voiceSettings
            };

            const scenarioExisted = !!PickeData.scenarioId;
            const scenarioMethod = scenarioExisted ? 'PUT' : 'POST';
            const scenarioUrl = scenarioExisted ? `/api/v1/admin/scenarios/${PickeData.scenarioId}` : '/api/v1/admin/scenarios';

            const scenarioRes = await fetch(scenarioUrl, {
                method: scenarioMethod,
                headers: PickeData.getAuthHeaders(),
                body: JSON.stringify(scenarioPayload)
            });
            if (!scenarioRes.ok) throw new Error('시나리오 저장에 실패했습니다.');

            const scenarioData = await scenarioRes.json();
            if (!scenarioExisted) {
                const scenarioResult = scenarioData.result || scenarioData.data || {};
                PickeData.scenarioId = scenarioResult.scenarioId || scenarioResult.id || null;
            }

            if (scenarioExisted && PickeData.scenarioId) {
                const shouldPatchScenarioStatus =
                    action === 'PUBLISHED'
                    || action === 'PUBLISH'
                    || previousStatus !== resolvedStatus;
                if (shouldPatchScenarioStatus) {
                    const statusRes = await fetch(`/api/v1/admin/scenarios/${PickeData.scenarioId}`, {
                        method: 'PATCH',
                        headers: PickeData.getAuthHeaders(),
                        body: JSON.stringify({ status: resolvedStatus })
                    });
                    if (!statusRes.ok) throw new Error('시나리오 상태 업데이트에 실패했습니다.');
                }
            }
        }

        PickeData.currentStatus = resolvedStatus;

        if (loader) {
            loader.classList.add('hidden');
            loader.classList.remove('flex');
        }

        const modal = document.getElementById('custom-modal');
        if (!modal) {
            window.location.href = '/api/v1/admin/picke/list';
            return;
        }

        document.getElementById('custom-modal-title').innerText = '완료';
        document.getElementById('custom-modal-message').innerText = action === 'PENDING'
            ? '임시 저장되었습니다.'
            : action === 'EDIT'
                ? '수정이 완료되었습니다.'
                : '발행이 완료되었습니다.';

        modal.classList.remove('hidden');
        setTimeout(() => {
            modal.classList.add('opacity-100');
            modal.classList.remove('opacity-0');
        }, 10);

        document.getElementById('custom-modal-confirm').onclick = () => {
            window.location.href = '/api/v1/admin/picke/list';
        };
    } catch (e) {
        if (loader) {
            loader.classList.add('hidden');
            loader.classList.remove('flex');
        }
        console.error('저장 중 오류:', e);
        alert(`저장에 실패했습니다: ${e.message}`);
    }
};
