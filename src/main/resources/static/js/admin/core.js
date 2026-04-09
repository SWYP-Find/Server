const token = localStorage.getItem("adminToken");
if (!token) {
    alert("로그인이 필요합니다.");
    window.location.replace("/api/v1/admin/login");
}

const urlParams = new URLSearchParams(window.location.search);

window.PickeData = {
    currentContentId: urlParams.get('id'),
    currentTypeParam: (urlParams.get('type') || 'BATTLE').toUpperCase(),
    isEditMode: !!urlParams.get('id'),
    token: token,
    currentContentType: 'BATTLE',
    allTags: [],
    selections: { BASIC: [], A: [], B: [] },
    currentTagTarget: 'BASIC',
    tempSelections: [],
    uploadedFiles: { thumbnail: null, charA: null, charB: null },
    editingTagId: null,

    API: {
        TAGS:          `/api/v1/tags`,
        TAG_CREATE:    `/api/v1/admin/tags`,
        TAG_UPDATE:    (id) => `/api/v1/admin/tags/${id}`,
        TAG_DELETE:    (id) => `/api/v1/admin/tags/${id}`,
        BATTLE_CREATE: `/api/v1/admin/battles`,
        BATTLE_UPDATE: (id) => `/api/v1/admin/battles/${id}`,
        BATTLE_GET:    (id) => `/api/v1/battles/${id}`,
        FILE_UPLOAD:   `/api/v1/files/upload`,
    },

    getAuthHeaders: () => ({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${window.PickeData.token}`
    }),

    setValue: (id, value) => {
        const el = document.getElementById(id);
        if (el && value != null) el.value = value;
    },

    setPreviewImage: (bgId, placeholderId, targetImgId, url) => {
        const bg = document.getElementById(bgId);
        if (bg) { bg.style.backgroundImage = `url(${url})`; bg.style.opacity = '1'; }
        document.getElementById(placeholderId)?.classList.add('hidden');
        const t = document.getElementById(targetImgId);
        if (t) t.style.backgroundImage = `url(${url})`;
    }
};