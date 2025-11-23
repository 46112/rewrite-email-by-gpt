// Service Worker for Chrome Extension

// 확장프로그램 설치시 기본 설정
chrome.runtime.onInstalled.addListener(() => {
  chrome.storage.local.set({
    serverUrl: 'http://localhost:8080',
    recipient: '',
    tone: 'formal'
  });
  console.log('메일 다듬기 확장프로그램이 설치되었습니다.');
});

// 아이콘 클릭시 팝업 열기 (기본 동작)
chrome.action.onClicked.addListener((tab) => {
  // popup.html이 있으므로 자동으로 팝업이 열림
});
