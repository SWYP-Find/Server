const token = localStorage.getItem("adminToken");
if (!token) {
    alert("로그인이 필요합니다.");
    window.location.replace("/api/v1/admin/login");
}

const urlParams = new URLSearchParams(window.location.search);

window.PickeData = {
    currentContentId: urlParams.get('id'),
    currentTypeParam: (() => {
        const type = (urlParams.get('type') || 'BATTLE').toUpperCase();
        return type === 'VOTE' ? 'POLL' : type;
    })(),
    isEditMode: !!urlParams.get('id'),
    token,
    currentContentType: 'BATTLE',
    currentTargetDate: null,
    currentStatus: null,
    allTags: [],
    selections: {
        CATEGORY: [],
        BATTLE_A_PHILOSOPHER: [],
        BATTLE_A_VALUE: [],
        BATTLE_B_PHILOSOPHER: [],
        BATTLE_B_VALUE: []
    },
    currentTagTarget: 'CATEGORY',
    tempSelections: [],
    uploadedFiles: { thumbnail: null, charA: null, charB: null },
    editingTagId: null,

    API: {
        TAGS: '/api/v1/tags',
        TAG_CREATE: '/api/v1/admin/tags',
        TAG_UPDATE: (id) => `/api/v1/admin/tags/${id}`,
        TAG_DELETE: (id) => `/api/v1/admin/tags/${id}`,
        BATTLE_CREATE: '/api/v1/admin/battles',
        BATTLE_UPDATE: (id) => `/api/v1/admin/battles/${id}`,
        BATTLE_GET: (id) => `/api/v1/admin/battles/${id}`,
        QUIZ_CREATE: '/api/v1/admin/quizzes',
        QUIZ_UPDATE: (id) => `/api/v1/admin/quizzes/${id}`,
        QUIZ_GET: (id) => `/api/v1/admin/quizzes/${id}`,
        POLL_CREATE: '/api/v1/admin/polls',
        POLL_UPDATE: (id) => `/api/v1/admin/polls/${id}`,
        POLL_GET: (id) => `/api/v1/admin/polls/${id}`,
        FILE_UPLOAD: '/api/v1/files/upload',
        FILE_UPLOAD_LOCAL: '/api/v1/files/upload/local'
    },

    getAuthHeaders: () => ({
        'Content-Type': 'application/json',
        Authorization: `Bearer ${window.PickeData.token}`
    }),

    setValue: (id, value) => {
        const el = document.getElementById(id);
        if (el && value != null) el.value = value;
    },

    setPreviewImage: (bgId, placeholderId, targetImgId, url) => {
        const bg = document.getElementById(bgId);
        if (bg) {
            bg.style.backgroundImage = `url(${url})`;
            bg.style.opacity = '1';
        }
        document.getElementById(placeholderId)?.classList.add('hidden');
        const target = document.getElementById(targetImgId);
        if (target) target.style.backgroundImage = `url(${url})`;
    }
};
