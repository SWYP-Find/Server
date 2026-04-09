window.loadContent = async function() {
    if (!PickeData.isEditMode) return;
    try {
        const res = await fetch(PickeData.API.BATTLE_GET(PickeData.currentContentId), { headers: PickeData.getAuthHeaders() });
        if (!res.ok) return;

        const json = await res.json();
        const data = json.result ?? json.data ?? json;

        console.log("[STEP 3] 서버에서 받아온 상세 데이터:", data);
        const actualType = data.type || PickeData.currentTypeParam;
        document.querySelector(`[data-target="form-${actualType.toLowerCase()}"]`)?.click();
        PickeData.currentContentType = actualType;

        const info = data.battleInfo || {};

        console.log("[DEBUG] 이미지 경로 확인:", info.thumbnailUrl);

        // 배경 이미지 세팅 함수 (업로드 칸 + 모바일 미리보기 동시 적용)
        const setBgImage = (uploadBgId, placeholderId, previewId, url) => {
            if (!url) return;
            const uploadBg = document.getElementById(uploadBgId);
            const placeholder = document.getElementById(placeholderId);

            if (uploadBg) {
                uploadBg.style.backgroundImage = `url('${url}')`;
                uploadBg.style.setProperty('opacity', '1', 'important');
                uploadBg.style.setProperty('display', 'block', 'important');
                uploadBg.classList.remove('opacity-0');

                if (placeholder) {
                    placeholder.style.display = 'none';
                    placeholder.classList.add('hidden');
                }
            }
            // 우측 모바일 미리보기 적용
            const previewBg = document.getElementById(previewId);
            if (previewBg) previewBg.style.backgroundImage = `url('${url}')`;
        };

        const renderBadges = (type, containerId) => {
            const container = document.getElementById(containerId);
            if (!container) return;
            container.querySelectorAll('.tag-badge').forEach(el => el.remove());
            PickeData.selections[type].forEach(id => {
                const t = PickeData.allTags.find(x => (x.tagId || x.id) === id);
                if (!t) return;
                const b = document.createElement('div');
                b.className = "tag-badge group relative inline-flex items-center px-3 py-1.5 bg-gray-100 text-gray-600 border border-gray-200 rounded-full text-[10px] font-bold mr-2 mb-2 transition-all hover:bg-gray-200";
                b.innerHTML = `#${t.name} <span class="ml-2 cursor-pointer text-gray-400 hover:text-red-500 font-bold text-xs" onclick="removeTag('${type}', ${id})" data-tag-id="${id}">&times;</span>`;
                container.insertBefore(b, container.lastElementChild);
            });
        };

        if (actualType === 'BATTLE') {
            PickeData.setValue('content-title', info.title || data.title || '');
            const summaryText = typeof info.summary === 'string' ? info.summary : (typeof data.summary === 'string' ? data.summary : '');
            PickeData.setValue('content-summary', summaryText);
            PickeData.setValue('content-desc', data.description || '');

            const categoryTags = data.categoryTags || info.tags || [];
            if (categoryTags.length) {
                PickeData.selections.BASIC = categoryTags.map(t => t.tagId || t.id);
                renderBadges('BASIC', 'basic-tags-container');
            }

            // 썸네일 이미지 불러오기 & 미리보기 적용
            if (info.thumbnailUrl || data.thumbnailUrl) {
                PickeData.existingUrls.thumbnail = info.thumbnailUrl || data.thumbnailUrl;
                setBgImage('thumbnail-preview-bg', 'thumbnail-placeholder', 'intro-bg-img', PickeData.existingUrls.thumbnail);
            }

            const options = info.options || data.options || [];
            const a = options.find(o => o.label === 'A');
            const b = options.find(o => o.label === 'B');
            if (a) {
                PickeData.setValue('char-a-title', a.title);
                PickeData.setValue('char-a-rep', a.representative);
                PickeData.setValue('char-a-stance', a.stance);
                /*PickeData.setValue('char-a-quote', a.quote);*/

                // 인물 A 이미지 불러오기 & 미리보기 적용
                if (a.imageUrl) {
                    PickeData.existingUrls.charA = a.imageUrl;
                    setBgImage('char-a-img-bg', 'char-a-img-placeholder', 'intro-char-a-img', a.imageUrl);
                }
                if (a.tags) {
                    PickeData.selections.A = a.tags.map(t => t.tagId || t.id);
                    renderBadges('A', 'char-a-tags-container');
                }
            }
            if (b) {
                PickeData.setValue('char-b-title', b.title);
                PickeData.setValue('char-b-rep', b.representative);
                PickeData.setValue('char-b-stance', b.stance);
                PickeData.setValue('char-b-quote', b.quote); // 복구 완료!

                // 인물 B 이미지 불러오기 & 미리보기 적용
                if (b.imageUrl) {
                    PickeData.existingUrls.charB = b.imageUrl;
                    setBgImage('char-b-img-bg', 'char-b-img-placeholder', 'intro-char-b-img', b.imageUrl);
                }
                if (b.tags) {
                    PickeData.selections.B = b.tags.map(t => t.tagId || t.id);
                    renderBadges('B', 'char-b-tags-container');
                }
            }

            const titleEl = document.getElementById('preview-title-intro');
            if (titleEl) titleEl.innerHTML = ((info.title || data.title) || '').replace(/\n/g, '<br>');

            try {
                const scenRes = await fetch(`/api/v1/admin/battles/${PickeData.currentContentId}/scenario`, { headers: PickeData.getAuthHeaders() });
                if (scenRes.ok) {
                    const scenJson = await scenRes.json();
                    const scenario = scenJson.result || scenJson.data;

                    if (scenario && scenario.nodes && scenario.nodes.length > 0) {
                        PickeData.scenarioId = scenario.scenarioId || scenario.id || null;

                        const renderScripts = (containerId, scripts) => {
                            const container = document.getElementById(containerId);
                            if (!container) return;
                            container.innerHTML = '';
                            scripts.forEach(script => {
                                window.addScriptBlock(containerId, script.speakerType);
                                const block = container.lastElementChild;
                                block.querySelector('.script-text').value = script.text || '';
                            });
                        };

                        scenario.nodes.forEach(node => {
                            if (node.nodeName === 'START') {
                                renderScripts('start-node-container', node.scripts || []);
                                if (scenario.isInteractive && node.interactiveOptions) {
                                    window.addBranchBlock();
                                    PickeData.setValue('branch-a-label', node.interactiveOptions[0]?.label || '');
                                    PickeData.setValue('branch-b-label', node.interactiveOptions[1]?.label || '');
                                }
                            }
                            if (node.nodeName === 'BRANCH_A') renderScripts('branch-a-node-container', node.scripts || []);
                            if (node.nodeName === 'BRANCH_B') renderScripts('branch-b-node-container', node.scripts || []);
                            if (node.nodeName === 'CLOSING') renderScripts('closing-node-container', node.scripts || []);
                        });
                    }
                }
            } catch (e) { console.error("시나리오 로드 실패:", e); }

            if (window.updateChatPreview) window.updateChatPreview();

        }
        // [QUIZ] 질문과 선택지 구성 로직
        else if (actualType === 'QUIZ') {
            const question = info.title || data.title || '';
            PickeData.setValue('quiz-question', question);
            PickeData.setValue('quiz-desc', data.description || info.description || '');
            PickeData.setValue('quiz-perf-a', data.itemA || info.itemA || '');
            PickeData.setValue('quiz-detail-a', data.itemADesc || info.itemADesc || '');
            PickeData.setValue('quiz-perf-b', data.itemB || info.itemB || '');
            PickeData.setValue('quiz-detail-b', data.itemBDesc || info.itemBDesc || '');

            const opts = info.options || data.options || [];
            const optA = opts.find(o => o.label === 'A');
            const optB = opts.find(o => o.label === 'B');

            if (optA) {
                PickeData.setValue('quiz-o-text', optA.title || '');
                PickeData.setValue('quiz-o-desc', optA.stance || '');
                if (optA.isCorrect) document.getElementById('quiz-answer-a').checked = true;
            }
            if (optB) {
                PickeData.setValue('quiz-x-text', optB.title || '');
                PickeData.setValue('quiz-x-desc', optB.stance || '');
                if (optB.isCorrect) document.getElementById('quiz-answer-b').checked = true;
            }
        }
        // [VOTE] (복구 완료!)
        else if (actualType === 'VOTE') {
            PickeData.setValue('vote-q-prefix', data.titlePrefix || '');
            PickeData.setValue('vote-q-suffix', data.titleSuffix || '');
            PickeData.setValue('vote-desc', data.description || '');

            const opts = data.options || info.options || [];
            opts.forEach((opt, idx) => {
                PickeData.setValue(`vote-opt-${idx + 1}`, opt.title || '');
            });
        }

        // UI 강제 업데이트 및 미리보기 동기화
        document.querySelectorAll('textarea, input').forEach(el => {
            el.dispatchEvent(new Event('input', { bubbles: true }));
        });

        // 태그 및 채팅 미리보기 함수 호출
        if (window.updatePreviewTags) window.updatePreviewTags();
        if (window.updateChatPreview) window.updateChatPreview();
        if (window.refreshFormBadges) window.refreshFormBadges();

        // 버튼 상태 업데이트
        window.updateButtonStates(data.status || info.status);
        console.log(`${actualType} 로드 완료`);

    } catch (e) {
        console.error("loadContent 오류:", e);
    }
};

// 버튼 상태 제어 함수 (PENDING 및 오디오 재발행 버튼 제어 포함)
window.updateButtonStates = function(currentStatus) {
    const btnPending = document.getElementById('btn-save-pending');
    const btnPublish = document.getElementById('btn-save-publish');
    const btnRepublish = document.getElementById('btn-republish-audio');

    // 수정 모드 진입 시 (PENDING이든 PUBLISHED든 기본적으로 텍스트 수정 모드로 세팅)
    if (PickeData.isEditMode && btnPublish) {
        btnPublish.innerText = '수정하기 (텍스트)';
        btnPublish.onclick = () => window.saveContent('EDIT');
    }

    if (currentStatus === 'PUBLISHED') {
        // 1. 이미 발행된 상태면 PENDING(임시저장) 버튼 비활성화
        if (btnPending) {
            btnPending.disabled = true;
            btnPending.classList.add('opacity-50', 'cursor-not-allowed', 'bg-gray-200', 'text-gray-400');
            btnPending.title = "이미 발행된 콘텐츠는 임시저장할 수 없습니다.";
        }
        // 2. 오디오 재발행 버튼 노출
        if (btnRepublish) {
            btnRepublish.classList.remove('hidden');
        }
    } else if (currentStatus === 'PENDING') {
        // 1. PENDING 상태면 오디오 재발행 버튼 숨김 처리
        if (btnRepublish) {
            btnRepublish.classList.add('hidden');
        }
        // 2. 혹시 비활성화되어 있을 수 있는 임시저장 버튼 활성화
        if (btnPending) {
            btnPending.disabled = false;
            btnPending.classList.remove('opacity-50', 'cursor-not-allowed', 'bg-gray-200', 'text-gray-400');
            btnPending.title = "";
        }
    }
};

// 오디오 재발행 확인 창 (모달)
window.confirmRepublish = function() {
    const isConfirmed = confirm(
        "정말 오디오를 다시 생성하시겠습니까?\n\n" +
        "대본이 수정되었다면 새로운 내용으로 오디오가 덮어씌워지며, " +
        "TTS API 생성 비용과 시간이 소요될 수 있습니다."
    );

    // 관리자가 '확인'을 눌렀을 때만 PUBLISH(오디오 생성) 실행
    if (isConfirmed) {
        window.saveContent('PUBLISH');
    }
};