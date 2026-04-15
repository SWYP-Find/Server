window.TagTargetConfig = {
    CATEGORY: {
        containerIds: ['basic-tags-container'],
        allowedTypes: ['CATEGORY'],
        preview: true
    },
    BATTLE_A_PHILOSOPHER: {
        containerIds: ['battle-a-philosopher-tags-container'],
        allowedTypes: ['PHILOSOPHER']
    },
    BATTLE_A_VALUE: {
        containerIds: ['battle-a-value-tags-container'],
        allowedTypes: ['VALUE']
    },
    BATTLE_B_PHILOSOPHER: {
        containerIds: ['battle-b-philosopher-tags-container'],
        allowedTypes: ['PHILOSOPHER']
    },
    BATTLE_B_VALUE: {
        containerIds: ['battle-b-value-tags-container'],
        allowedTypes: ['VALUE']
    }
};

window.openTagSelectModal = (target) => {
    if (!PickeData.selections[target]) return;
    PickeData.currentTagTarget = target;
    PickeData.tempSelections = [...PickeData.selections[target]];
    const searchInput = document.getElementById('tag-search-input');
    if (searchInput) searchInput.value = '';
    window.renderTagModalList();
    document.getElementById('tag-select-modal')?.classList.remove('hidden');
};

window.closeTagSelectModal = () => {
    document.getElementById('tag-select-modal')?.classList.add('hidden');
};

window.toggleTagSelection = (tagId) => {
    PickeData.tempSelections = PickeData.tempSelections.includes(tagId)
        ? PickeData.tempSelections.filter((id) => id !== tagId)
        : [...PickeData.tempSelections, tagId];
    window.renderTagModalList(document.getElementById('tag-search-input')?.value || '');
};

window.renderTagModalList = function (searchQuery = '') {
    const container = document.getElementById('tag-list-container');
    if (!container) return;

    const currentConfig = window.TagTargetConfig[PickeData.currentTagTarget] || window.TagTargetConfig.CATEGORY;
    const allowedTypes = currentConfig.allowedTypes || [];

    container.innerHTML = '';
    const filtered = PickeData.allTags.filter((tag) => {
        const name = String(tag.name || '');
        const matchedName = name.includes(searchQuery);
        const matchedType = allowedTypes.length === 0 || allowedTypes.includes(tag.type);
        return matchedName && matchedType;
    });

    const groups = [
        { title: 'CATEGORY', type: 'CATEGORY' },
        { title: 'PHILOSOPHER', type: 'PHILOSOPHER' },
        { title: 'VALUE', type: 'VALUE' }
    ];

    groups
        .filter((group) => allowedTypes.length === 0 || allowedTypes.includes(group.type))
        .forEach((group) => {
            const tags = filtered.filter((tag) => tag.type === group.type);
            if (!tags.length) return;

            let html = `<div class="mb-5"><h4 class="text-[11px] font-black text-gray-400 mb-3 uppercase tracking-tighter">${group.title}</h4><div class="flex flex-wrap gap-2">`;
            tags.forEach((tag) => {
                const tagId = tag.tagId || tag.id;
                const selected = PickeData.tempSelections.includes(tagId);
                html += `<div class="relative group">
                    <button type="button" onclick="toggleTagSelection(${tagId})" class="px-4 py-1.5 rounded-full text-xs font-bold border ${selected ? 'bg-black text-white border-black' : 'bg-white text-gray-600 border-gray-200'}">#${tag.name}</button>
                    <div class="absolute -top-1.5 -right-1.5 hidden group-hover:flex gap-1 z-10">
                        <button type="button" onclick="event.stopPropagation(); window.updateTagName(${tagId})" class="w-4 h-4 bg-blue-500 text-white rounded-full text-[8px] flex items-center justify-center shadow-sm">E</button>
                        <button type="button" onclick="event.stopPropagation(); window.deleteTag(${tagId})" class="w-4 h-4 bg-red-500 text-white rounded-full text-[8px] flex items-center justify-center shadow-sm">X</button>
                    </div>
                </div>`;
            });
            container.innerHTML += `${html}</div></div>`;
        });
};

window.confirmTagSelection = () => {
    const target = PickeData.currentTagTarget;
    PickeData.selections[target] = [...PickeData.tempSelections];
    window.refreshFormBadges(target);

    if (window.TagTargetConfig[target]?.preview && window.updatePreviewTags) {
        window.updatePreviewTags();
    }

    window.closeTagSelectModal();
};

window.refreshFormBadges = (specificTarget = null) => {
    const targets = specificTarget ? [specificTarget] : Object.keys(window.TagTargetConfig);

    targets.forEach((target) => {
        const config = window.TagTargetConfig[target];
        if (!config) return;

        config.containerIds.forEach((containerId) => {
            const container = document.getElementById(containerId);
            if (!container) return;

            container.querySelectorAll('.tag-badge').forEach((el) => el.remove());

            (PickeData.selections[target] || []).forEach((tagId) => {
                const tag = PickeData.allTags.find((item) => (item.tagId || item.id) === tagId);
                if (!tag) return;

                const badge = document.createElement('div');
                badge.className = 'tag-badge group relative inline-flex items-center px-3 py-1.5 bg-gray-100 text-gray-600 border border-gray-200 rounded-full text-[10px] font-bold mr-2 mb-2 transition-all hover:bg-gray-200';
                badge.innerHTML = `#${tag.name}<span class="ml-2 cursor-pointer text-gray-400 hover:text-red-500 font-bold text-xs" onclick="removeTag('${target}', ${tagId})" data-tag-id="${tagId}">&times;</span>`;
                container.insertBefore(badge, container.lastElementChild);
            });
        });
    });
};

window.removeTag = function (target, tagId) {
    if (!PickeData.selections[target]) return;

    PickeData.selections[target] = PickeData.selections[target].filter((id) => id !== tagId);
    window.refreshFormBadges(target);

    if (window.TagTargetConfig[target]?.preview && window.updatePreviewTags) {
        window.updatePreviewTags();
    }
};

window.openTagCreateModal = () => {
    PickeData.editingTagId = null;
    document.querySelector('#tag-modal h2').innerText = '새 태그 생성';
    document.getElementById('tag-name-input').value = '';
    document.getElementById('tag-type-select').value = 'CATEGORY';
    document.getElementById('tag-select-modal')?.classList.add('hidden');
    document.getElementById('tag-modal')?.classList.remove('hidden');
};

window.updateTagName = function (tagId) {
    const tag = PickeData.allTags.find((item) => item.tagId === tagId || item.id === tagId);
    if (!tag) return;

    PickeData.editingTagId = tagId;
    document.querySelector('#tag-modal h2').innerText = '태그 수정';
    document.getElementById('tag-name-input').value = tag.name;
    document.getElementById('tag-type-select').value = tag.type;
    document.getElementById('tag-select-modal')?.classList.add('hidden');
    document.getElementById('tag-modal')?.classList.remove('hidden');
};

window.updatePreviewTags = function () {
    const box = document.getElementById('preview-tags');
    if (!box) return;

    box.innerHTML = (PickeData.selections.CATEGORY || [])
        .map((tagId) => {
            const tag = PickeData.allTags.find((item) => (item.tagId || item.id) === tagId);
            return tag
                ? `<span class="bg-[#F5F0EA] text-[#8B7355] px-2 py-1 rounded text-[9px] font-bold border border-[#E5E0D8]">#${tag.name}</span>`
                : '';
        })
        .join('');
};

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('tag-search-input')?.addEventListener('input', (e) => {
        window.renderTagModalList(e.target.value.trim());
    });
});
