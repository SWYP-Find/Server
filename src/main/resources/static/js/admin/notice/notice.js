document.addEventListener("DOMContentLoaded", () => {
    const token = localStorage.getItem("adminToken");
    if (!token) {
        alert("로그인이 필요합니다.");
        window.location.replace("/api/v1/admin/login");
        return;
    }

    const api = {
        list: (page = 0, size = 20, category = "ALL") => {
            const params = new URLSearchParams({ page: String(page), size: String(size) });
            if (category && category !== "ALL") {
                params.set("category", category);
            }
            return `/api/v1/admin/notices?${params.toString()}`;
        },
        create: "/api/v1/admin/notices"
    };

    const categoryLabelMap = {
        ALL: "전체",
        CONTENT: "콘텐츠",
        NOTICE: "공지사항",
        EVENT: "이벤트"
    };

    const tbody = document.getElementById("notice-list-tbody");
    const form = document.getElementById("notice-form");
    const refreshButton = document.getElementById("refresh-notice-list");
    const createCategoryButtons = Array.from(document.querySelectorAll(".create-category-btn"));
    const filterCategoryButtons = Array.from(document.querySelectorAll(".filter-category-btn"));

    let currentCreateCategory = "NOTICE";
    let currentFilterCategory = "ALL";

    const authHeaders = () => ({
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`
    });

    const renderDate = (dateTime) => {
        if (!dateTime) return "-";
        return new Date(dateTime).toLocaleString("ko-KR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        });
    };

    const renderCategory = (category) => categoryLabelMap[category] || category;

    const applyButtonState = (buttons, activeValue, dataKey) => {
        buttons.forEach((button) => {
            const value = button.dataset[dataKey];
            const isActive = value === activeValue;
            button.classList.toggle("border-black", isActive);
            button.classList.toggle("bg-black", isActive);
            button.classList.toggle("text-white", isActive);
            button.classList.toggle("border-gray-200", !isActive);
            button.classList.toggle("bg-gray-50", !isActive);
            button.classList.toggle("text-gray-600", !isActive);
        });
    };

    const renderRows = (items) => {
        tbody.innerHTML = "";
        if (!items || items.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="px-4 py-10 text-center text-gray-400">등록된 공지가 없습니다.</td></tr>`;
            return;
        }

        items.forEach((item) => {
            const tr = document.createElement("tr");
            tr.className = "hover:bg-gray-50";
            tr.innerHTML = `
                <td class="px-4 py-3 text-gray-500 font-mono">${item.notificationId}</td>
                <td class="px-4 py-3">
                    <span class="px-2 py-1 rounded text-[10px] font-bold ${item.category === "EVENT" ? "bg-blue-100 text-blue-700" : item.category === "CONTENT" ? "bg-violet-100 text-violet-700" : "bg-green-100 text-green-700"}">
                        ${renderCategory(item.category)}
                    </span>
                </td>
                <td class="px-4 py-3 font-bold text-gray-900">${item.title || "-"}</td>
                <td class="px-4 py-3 text-xs text-gray-500">${renderDate(item.createdAt)}</td>
            `;
            tbody.appendChild(tr);
        });
    };

    const loadNotices = async () => {
        tbody.innerHTML = `<tr><td colspan="4" class="px-4 py-10 text-center text-gray-400">불러오는 중...</td></tr>`;
        try {
            const res = await fetch(api.list(0, 20, currentFilterCategory), { headers: authHeaders() });
            if (res.status === 401 || res.status === 403) {
                alert("세션이 만료되었습니다. 다시 로그인해 주세요.");
                window.location.replace("/api/v1/admin/login");
                return;
            }
            if (!res.ok) throw new Error(`공지 목록 조회 실패 (HTTP ${res.status})`);
            const json = await res.json();
            const data = json.result || json.data || {};
            renderRows(data.items || []);
        } catch (e) {
            console.error("공지 목록 조회 오류:", e);
            tbody.innerHTML = `<tr><td colspan="4" class="px-4 py-10 text-center text-red-500">공지 목록 로드에 실패했습니다.</td></tr>`;
        }
    };

    createCategoryButtons.forEach((button) => {
        button.addEventListener("click", () => {
            currentCreateCategory = button.dataset.createCategory || "NOTICE";
            applyButtonState(createCategoryButtons, currentCreateCategory, "createCategory");
        });
    });

    filterCategoryButtons.forEach((button) => {
        button.addEventListener("click", async () => {
            currentFilterCategory = button.dataset.filterCategory || "ALL";
            applyButtonState(filterCategoryButtons, currentFilterCategory, "filterCategory");
            await loadNotices();
        });
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();

        const title = document.getElementById("notice-title")?.value?.trim() || "";
        const body = document.getElementById("notice-body")?.value?.trim() || "";

        if (!title || !body) {
            alert("제목과 내용을 입력해 주세요.");
            return;
        }

        const payload = { category: currentCreateCategory, title, body };

        try {
            const res = await fetch(api.create, {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error(`공지 저장 실패 (HTTP ${res.status})`);

            document.getElementById("notice-title").value = "";
            document.getElementById("notice-body").value = "";
            alert("공지사항이 등록되었습니다.");
            await loadNotices();
        } catch (e) {
            console.error("공지 저장 오류:", e);
            alert("공지 저장에 실패했습니다.");
        }
    });

    refreshButton?.addEventListener("click", loadNotices);

    applyButtonState(createCategoryButtons, currentCreateCategory, "createCategory");
    applyButtonState(filterCategoryButtons, currentFilterCategory, "filterCategory");
    loadNotices();
});