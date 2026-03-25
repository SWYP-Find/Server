window.fetchAllTags = async function () {
    try {
        const res  = await fetch(PickeData.API.TAGS, { headers: PickeData.getAuthHeaders() });
        if (!res.ok) throw new Error(res.status);
        const json = await res.json();
        PickeData.allTags = json.result?.items ?? json.data?.items ?? [];
        window.renderTagModalList();
    } catch (e) {
        console.error("태그 로드 실패:", e);
    }
};

window.submitNewTag = async () => {
    const nameInput  = document.getElementById('tag-name-input');
    const typeSelect = document.getElementById('tag-type-select');
    if (!nameInput || !typeSelect) return;
    const name = nameInput.value.trim();
    const type = typeSelect.value;
    if (!name) { alert("태그 이름을 입력하세요."); nameInput.focus(); return; }

    if (PickeData.editingTagId) {
        try {
            const res = await fetch(PickeData.API.TAG_UPDATE(PickeData.editingTagId), {
                method: 'PATCH',
                headers: PickeData.getAuthHeaders(),
                body: JSON.stringify({ name, type })
            });
            if (res.ok) {
                await window.fetchAllTags();
                window.refreshFormBadges();
                if(window.updatePreviewTags) window.updatePreviewTags();
                document.getElementById('tag-modal')?.classList.add('hidden');
                document.getElementById('tag-select-modal')?.classList.remove('hidden');
            }
        } catch (e) { alert("수정 중 서버 오류 발생"); }
    } else {
        try {
            const res = await fetch(PickeData.API.TAG_CREATE, {
                method: 'POST',
                headers: PickeData.getAuthHeaders(),
                body: JSON.stringify({ name, type })
            });
            if (res.ok) {
                await window.fetchAllTags();
                document.getElementById('tag-modal')?.classList.add('hidden');
                document.getElementById('tag-select-modal')?.classList.remove('hidden');
                nameInput.value = '';
            }
        } catch (e) { alert("생성 중 서버 오류 발생"); }
    }
};

window.deleteTag = async function (tagId) {
    if (!confirm("태그를 삭제하시겠습니까? (사용 중인 경우 삭제되지 않습니다.)")) return;
    try {
        const res = await fetch(PickeData.API.TAG_DELETE(tagId), {
            method: 'DELETE',
            headers: PickeData.getAuthHeaders()
        });
        if (res.ok) {
            ['BASIC', 'A', 'B'].forEach(type => {
                if (PickeData.selections[type].includes(tagId)) window.removeTag(type, tagId);
            });
            PickeData.tempSelections = PickeData.tempSelections.filter(id => id !== tagId);
            await window.fetchAllTags();
        } else {
            const err = await res.json();
            alert(err.message || "삭제할 수 없는 태그입니다.");
        }
    } catch (e) { console.error("태그 삭제 오류:", e); }
};