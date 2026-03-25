PickeData.existingUrls = { thumbnail: null, charA: null, charB: null };
PickeData.scenarioId = null;

// [공통] 이미지 서버 업로드 함수
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
    } catch (e) { console.error("이미지 업로드 실패:", e); return null; }
};

// [공통] 페이지 로드 시 초기화 세팅
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
            if(window.updateChatPreview) window.updateChatPreview();
        });
    }

    setupImageUpload('thumbnail-upload', 'thumbnail-preview-bg', 'thumbnail-placeholder', 'intro-bg-img', 'thumbnail');
    setupImageUpload('char-a-img-upload', 'char-a-img-bg', 'char-a-img-placeholder', 'intro-char-a-img', 'charA');
    setupImageUpload('char-b-img-upload', 'char-b-img-bg', 'char-b-img-placeholder', 'intro-char-b-img', 'charB');

    if (typeof window.fetchAllTags === 'function') await window.fetchAllTags();
    if (typeof window.loadContent === 'function') await window.loadContent();
});