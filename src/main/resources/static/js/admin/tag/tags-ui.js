

// 태그 선택 모달 열기
window.openTagSelectModal = (target) => {
    PickeData.currentTagTarget = target;
    PickeData.tempSelections = [...PickeData.selections[target]];
    if (document.getElementById('tag-search-input')) document.getElementById('tag-search-input').value = '';
    window.renderTagModalList();
    document.getElementById('tag-select-modal')?.classList.remove('hidden');
};

// 모달 닫기
window.closeTagSelectModal = () => {
    document.getElementById('tag-select-modal')?.classList.add('hidden');
};

// 태그 선택 토글
window.toggleTagSelection = (tagId) => {
    PickeData.tempSelections = PickeData.tempSelections.includes(tagId)
        ? PickeData.tempSelections.filter(id => id !== tagId)
        : [...PickeData.tempSelections, tagId];
    window.renderTagModalList(document.getElementById('tag-search-input')?.value || "");
};

window.renderTagModalList = function (searchQuery = "") {
    const container = document.getElementById('tag-list-container');
    if (!container) return;
    container.innerHTML = '';
    const filtered = PickeData.allTags.filter(t => t.name.includes(searchQuery));
    const groups = [{ t: "카테고리", v: "CATEGORY" }, { t: "철학자", v: "PHILOSOPHER" }, { t: "가치관", v: "VALUE" }];

    groups.forEach(group => {
        const tags = filtered.filter(t => t.type === group.v);
        if (!tags.length) return;
        let html = `<div class="mb-5"><h4 class="text-[11px] font-black text-gray-400 mb-3 uppercase tracking-tighter">${group.t}</h4><div class="flex flex-wrap gap-2">`;
        tags.forEach(tag => {
            const tid = tag.tagId || tag.id;
            const selected = PickeData.tempSelections.includes(tid);
            html += `<div class="relative group">
                <button type="button" onclick="toggleTagSelection(${tid})" class="px-4 py-1.5 rounded-full text-xs font-bold border ${selected ? 'bg-black text-white border-black' : 'bg-white text-gray-600 border-gray-200'}">#${tag.name}</button>
                <div class="absolute -top-1.5 -right-1.5 hidden group-hover:flex gap-1 z-10">
                    <button type="button" onclick="event.stopPropagation(); window.updateTagName(${tid})" class="w-4 h-4 bg-blue-500 text-white rounded-full text-[8px] flex items-center justify-center shadow-sm">✎</button>
                    <button type="button" onclick="event.stopPropagation(); window.deleteTag(${tid})" class="w-4 h-4 bg-red-500 text-white rounded-full text-[8px] flex items-center justify-center shadow-sm">×</button>
                </div></div>`;
        });
        container.innerHTML += html + `</div></div>`;
    });
};

window.confirmTagSelection = () => {
    const target = PickeData.currentTagTarget;
    PickeData.selections[target] = [...PickeData.tempSelections];
    window.refreshFormBadges(target);
    if (target === 'BASIC' && window.updatePreviewTags) window.updatePreviewTags();
    window.closeTagSelectModal();
};

window.refreshFormBadges = (specificTarget = null) => {
    const targets = specificTarget ? [specificTarget] : ['BASIC', 'A', 'B'];
    targets.forEach(target => {
        const cid = target === 'BASIC' ? 'basic-tags-container' : `char-${target.toLowerCase()}-tags-container`;
        const container = document.getElementById(cid);
        if (!container) return;
        container.querySelectorAll('.tag-badge').forEach(el => el.remove());
        PickeData.selections[target].forEach(tagId => {
            const tag = PickeData.allTags.find(t => (t.tagId || t.id) === tagId);
            if (!tag) return;
            const b = document.createElement('div');
            b.className = "tag-badge group relative inline-flex items-center px-3 py-1.5 bg-gray-100 text-gray-600 border border-gray-200 rounded-full text-[10px] font-bold mr-2 mb-2 transition-all hover:bg-gray-200";
            b.innerHTML = `#${tag.name}<span class="ml-2 cursor-pointer text-gray-400 hover:text-red-500 font-bold text-xs" onclick="removeTag('${target}', ${tagId})" data-tag-id="${tagId}">&times;</span>`;
            container.insertBefore(b, container.lastElementChild);
        });
    });
};

// 배지에서 X 눌렀을 때 삭제 로직
window.removeTag = function (type, tagId) {
    PickeData.selections[type] = PickeData.selections[type].filter(id => id !== tagId);
    window.refreshFormBadges(type);
    if (type === 'BASIC' && window.updatePreviewTags) window.updatePreviewTags();
};

// 태그 생성 모달 열기
window.openTagCreateModal = () => {
    PickeData.editingTagId = null;
    document.querySelector('#tag-modal h2').innerText = "새 태그 추가";
    document.getElementById('tag-name-input').value = '';
    document.getElementById('tag-type-select').value = 'CATEGORY';
    document.getElementById('tag-select-modal')?.classList.add('hidden');
    document.getElementById('tag-modal')?.classList.remove('hidden');
};

// 태그 수정 모달 열기
window.updateTagName = function (tagId) {
    const tag = PickeData.allTags.find(t => t.tagId === tagId || t.id === tagId);
    if (!tag) return;
    PickeData.editingTagId = tagId;
    document.querySelector('#tag-modal h2').innerText = "태그 수정";
    document.getElementById('tag-name-input').value = tag.name;
    document.getElementById('tag-type-select').value = tag.type;
    document.getElementById('tag-select-modal')?.classList.add('hidden');
    document.getElementById('tag-modal')?.classList.remove('hidden');
};

window.updatePreviewTags = function () {
    const box = document.getElementById('preview-tags');
    if (!box) return;

    // BASIC(배틀 전체 태그) 그룹의 태그들을 미리보기 하단에 렌더링
    box.innerHTML = PickeData.selections.BASIC.map(tagId => {
        const tag = PickeData.allTags.find(t => (t.tagId || t.id) === tagId);
        return tag
            ? `<span class="bg-[#F5F0EA] text-[#8B7355] px-2 py-1 rounded text-[9px] font-bold border border-[#E5E0D8]">#${tag.name}</span>`
            : '';
    }).join('');
};

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('tag-search-input')?.addEventListener('input', (e) => {
        window.renderTagModalList(e.target.value.trim());
    });
});