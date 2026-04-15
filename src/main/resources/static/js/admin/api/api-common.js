PickeData.existingUrls = { thumbnail: null, charA: null, charB: null };
PickeData.scenarioId = null;

window.applyThumbnailPreview = function(url) {
    if (!url) return;

    const bgIds = ['thumbnail-preview-bg'];
    const placeholderIds = ['thumbnail-placeholder'];
    const previewIds = ['intro-bg-img'];

    bgIds.forEach((id) => {
        const bg = document.getElementById(id);
        if (!bg) return;
        bg.style.backgroundImage = `url('${url}')`;
        bg.style.setProperty('opacity', '1', 'important');
        bg.classList.remove('opacity-0');
    });

    placeholderIds.forEach((id) => {
        const placeholder = document.getElementById(id);
        if (placeholder) {
            placeholder.style.display = 'none';
            placeholder.classList.add('hidden');
        }
    });

    previewIds.forEach((id) => {
        const target = document.getElementById(id);
        if (target) target.style.backgroundImage = `url('${url}')`;
    });
};

window.setTargetDateInputs = function(dateValue) {
    if (!dateValue) return;
    ['battle-target-date', 'quiz-target-date', 'poll-target-date'].forEach((id) => {
        const input = document.getElementById(id);
        if (input) input.value = dateValue;
    });
};

window.uploadImageToServer = async function(file, category) {
    if (!file) return null;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('category', category);

    try {
        const res = await fetch(PickeData.API.FILE_UPLOAD, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${PickeData.token}` },
            body: formData
        });
        if (!res.ok) throw new Error(res.status);
        const text = await res.text();
        try { return JSON.parse(text).result ?? JSON.parse(text).data ?? text; }
        catch { return text; }
    } catch (e) {
        console.error("이미지 업로드 실패:", e);
        return null;
    }
};

window.uploadImageToLocalDraft = async function(file) {
    if (!file) return null;
    const formData = new FormData();
    formData.append('file', file);

    try {
        const res = await fetch(PickeData.API.FILE_UPLOAD_LOCAL, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${PickeData.token}` },
            body: formData
        });
        if (!res.ok) throw new Error(res.status);
        const text = await res.text();
        try { return JSON.parse(text).result ?? JSON.parse(text).data ?? text; }
        catch { return text; }
    } catch (e) {
        console.error("로컬 임시 이미지 업로드 실패:", e);
        return null;
    }
};

document.addEventListener("DOMContentLoaded", async () => {
    function setupImageUpload(inputId, bgId, placeholderId, targetImgId, fileKey) {
        const input = document.getElementById(inputId);
        if (!input) return;
        input.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;
            PickeData.uploadedFiles[fileKey] = file;
            const url = URL.createObjectURL(file);
            PickeData.setPreviewImage(bgId, placeholderId, targetImgId, url);
            if (window.updateChatPreview) window.updateChatPreview();
        });
    }

    function setupThumbnailUpload(inputId) {
        const input = document.getElementById(inputId);
        if (!input) return;
        input.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;
            PickeData.uploadedFiles.thumbnail = file;
            const url = URL.createObjectURL(file);
            window.applyThumbnailPreview(url);
        });
    }

    function bindTargetDateInput(inputId) {
        const input = document.getElementById(inputId);
        if (!input) return;
        input.addEventListener('change', (e) => {
            const value = e.target.value;
            if (!value) return;
            PickeData.currentTargetDate = value;
            window.setTargetDateInputs(value);
        });
    }

    setupThumbnailUpload('thumbnail-upload');
    setupImageUpload('char-a-img-upload', 'char-a-img-bg', 'char-a-img-placeholder', 'intro-char-a-img', 'charA');
    setupImageUpload('char-b-img-upload', 'char-b-img-bg', 'char-b-img-placeholder', 'intro-char-b-img', 'charB');

    bindTargetDateInput('battle-target-date');
    bindTargetDateInput('quiz-target-date');
    bindTargetDateInput('poll-target-date');

    const today = new Date().toISOString().split('T')[0];
    if (!PickeData.currentTargetDate) PickeData.currentTargetDate = today;
    window.setTargetDateInputs(PickeData.currentTargetDate);

    if (typeof window.fetchAllTags === 'function') await window.fetchAllTags();
    if (typeof window.loadContent === 'function') await window.loadContent();
});
