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

        const info = data.summary || data.battleInfo || data;

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

            const tagsArray = info.tags || data.tags || [];
            if (tagsArray.length) {
                PickeData.selections.BASIC = tagsArray.map(t => t.tagId || t.id);
                renderBadges('BASIC', 'basic-tags-container');
            }
            if (info.thumbnailUrl || data.thumbnailUrl) {
                PickeData.existingUrls.thumbnail = info.thumbnailUrl || data.thumbnailUrl;
                PickeData.setPreviewImage('thumbnail-preview-bg', 'thumbnail-placeholder', 'intro-bg-img', PickeData.existingUrls.thumbnail);
            }

            const options = info.options || data.options || [];
            const a = options.find(o => o.label === 'A');
            const b = options.find(o => o.label === 'B');
            if (a) {
                PickeData.setValue('char-a-title', a.title);
                PickeData.setValue('char-a-rep', a.representative);
                PickeData.setValue('char-a-stance', a.stance);
                PickeData.setValue('char-a-quote', a.quote);
                if (a.imageUrl) {
                    PickeData.existingUrls.charA = a.imageUrl;
                    PickeData.setPreviewImage('char-a-img-bg', 'char-a-img-placeholder', 'intro-char-a-img', a.imageUrl);
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
                PickeData.setValue('char-b-quote', b.quote);
                if (b.imageUrl) {
                    PickeData.existingUrls.charB = b.imageUrl;
                    PickeData.setPreviewImage('char-b-img-bg', 'char-b-img-placeholder', 'intro-char-b-img', b.imageUrl);
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
            // 1. 데이터 근원지 확보 (battleInfo)
            const info = data.battleInfo || data;

            // 2. [질문] 로드 (battleInfo 안에 title이 있음)
            const question = info.title || data.title || '';
            PickeData.setValue('quiz-question', question);

            // 3. [형식소개/설명] 로드 (로그상 description은 바깥에 있음)
            PickeData.setValue('quiz-desc', data.description || info.description || '');

            // 4. [공연A/B] 로드 (추가하신 필드들)
            PickeData.setValue('quiz-perf-a', data.itemA || info.itemA || '');
            PickeData.setValue('quiz-detail-a', data.itemADesc || info.itemADesc || '');
            PickeData.setValue('quiz-perf-b', data.itemB || info.itemB || '');
            PickeData.setValue('quiz-detail-b', data.itemBDesc || info.itemBDesc || '');

            // 5. [선택지 구성] 로드 (info.options 배열에서 label A, B를 찾아 매핑)
            const opts = info.options || data.options || [];
            const optA = opts.find(o => o.label === 'A');
            const optB = opts.find(o => o.label === 'B');

            if (optA) {
                PickeData.setValue('quiz-o-text', optA.title || ''); // O 정답 텍스트
                PickeData.setValue('quiz-o-desc', optA.stance || ''); // O 정답 설명
            }
            if (optB) {
                PickeData.setValue('quiz-x-text', optB.title || ''); // X 오답 텍스트
                PickeData.setValue('quiz-x-desc', optB.stance || ''); // X 오답 설명
            }

            console.log("[최종 검증] 질문:", question, " / 선택지A:", optA?.title);
        }

        // [VOTE]
        else if (actualType === 'VOTE') {
            PickeData.setValue('vote-q-prefix', data.titlePrefix || '');
            PickeData.setValue('vote-q-suffix', data.titleSuffix || '');
            PickeData.setValue('vote-desc', data.description || '');

            const opts = data.options || info.options || [];
            opts.forEach((opt, idx) => {
                PickeData.setValue(`vote-opt-${idx + 1}`, opt.title || '');
            });
        }

        // UI 최종 갱신
        if (window.refreshFormBadges) window.refreshFormBadges();
        if (window.updatePreviewTags) window.updatePreviewTags();
        console.log(`${actualType} 로드 완료`);

    } catch (e) {
        console.error("loadContent 오류:", e);
    }
};