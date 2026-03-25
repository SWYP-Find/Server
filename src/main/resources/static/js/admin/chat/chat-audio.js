function _getAudio() { return document.getElementById('preview-audio'); }

window.toggleAudio = function () {
    const a = _getAudio();
    if (!a) return;
    if (!a.src || a.src === window.location.href) { _openAudioPicker(); return; }
    a.paused ? a.play() : a.pause();
};

function _updatePlayIcon() {
    const a = _getAudio();
    const icon = document.getElementById('audio-play-icon');
    if (!icon) return;
    icon.setAttribute('d', (a && !a.paused) ? 'M6 19h4V5H6v14zm8-14v14h4V5h-4z' : 'M8 5v14l11-7z');
}

// 파일 선택 팝업
function _openAudioPicker() {
    let picker = document.getElementById('audio-file-picker') || document.createElement('input');
    picker.type = 'file'; picker.accept = 'audio/*'; picker.id = 'audio-file-picker';
    picker.style.display = 'none';
    if (!document.getElementById('audio-file-picker')) document.body.appendChild(picker);

    picker.onchange = (e) => {
        const file = e.target.files[0];
        if (file) {
            const a = _getAudio();
            a.src = URL.createObjectURL(file);
            a.play();
        }
    };
    picker.click();
}

// 이벤트 리스너 등록
document.addEventListener('DOMContentLoaded', () => {
    const a = _getAudio();
    if (!a) return;

    a.addEventListener('timeupdate', () => {
        const pct = a.duration ? (a.currentTime / a.duration * 100) : 0;
        if (document.getElementById('audio-progress-fill')) document.getElementById('audio-progress-fill').style.width = pct + '%';
        if (document.getElementById('audio-current-time')) document.getElementById('audio-current-time').textContent = Math.floor(a.currentTime / 60) + ":" + Math.floor(a.currentTime % 60).toString().padStart(2, '0');
    });

    a.addEventListener('play', _updatePlayIcon);
    a.addEventListener('pause', _updatePlayIcon);
});