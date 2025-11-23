// 네이버 메일 페이지에서 실행되는 content script

(function() {
  'use strict';

  // 메시지 리스너 등록
  chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'getMailContent') {
      const content = getMailContent();
      sendResponse({ content });
    } else if (request.action === 'updateMailContent') {
      const success = updateMailContent(request.content);
      sendResponse({ success });
    }
    return true; // 비동기 응답을 위해 true 반환
  });

  // 메일 내용 가져오기
  function getMailContent() {
    // 네이버 메일 에디터의 다양한 선택자 시도
    const selectors = [
      // 새 메일 작성 에디터 (iframe 내부)
      'iframe[id*="editor"]',
      'iframe[class*="editor"]',
      // 직접 에디터
      '.se-content',
      '[contenteditable="true"]',
      'textarea[name*="body"]',
      'textarea[name*="content"]',
      '#body',
      '.mail_body'
    ];

    // iframe 내부 확인
    const iframes = document.querySelectorAll('iframe');
    for (const iframe of iframes) {
      try {
        const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
        const body = iframeDoc.body;
        if (body && body.innerHTML.trim()) {
          // HTML 태그 제거하고 텍스트만 추출
          const text = body.innerText || body.textContent;
          if (text && text.trim()) {
            return text.trim();
          }
        }
      } catch (e) {
        // Cross-origin iframe 접근 실패
        console.log('iframe 접근 실패:', e.message);
      }
    }

    // 일반 선택자로 시도
    for (const selector of selectors) {
      const element = document.querySelector(selector);
      if (element) {
        if (element.tagName === 'IFRAME') {
          try {
            const doc = element.contentDocument || element.contentWindow.document;
            const text = doc.body.innerText || doc.body.textContent;
            if (text && text.trim()) {
              return text.trim();
            }
          } catch (e) {
            continue;
          }
        } else if (element.tagName === 'TEXTAREA') {
          if (element.value && element.value.trim()) {
            return element.value.trim();
          }
        } else {
          const text = element.innerText || element.textContent;
          if (text && text.trim()) {
            return text.trim();
          }
        }
      }
    }

    return null;
  }

  // 메일 내용 업데이트
  function updateMailContent(newContent) {
    console.log('[메일 다듬기] 업데이트 시작, 내용 길이:', newContent.length);

    // iframe 내부 확인 (네이버 메일은 주로 iframe 사용)
    const iframes = document.querySelectorAll('iframe');
    console.log('[메일 다듬기] 찾은 iframe 개수:', iframes.length);

    for (const iframe of iframes) {
      try {
        const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
        const body = iframeDoc.body;

        // body에 내용이 있으면 업데이트 (isContentEditable 체크 제거)
        if (body && body.innerHTML && body.innerHTML.trim()) {
          console.log('[메일 다듬기] iframe body 발견, 업데이트 시도');
          // 줄바꿈을 <br>로 변환
          body.innerHTML = newContent.replace(/\n/g, '<br>');

          // 변경 이벤트 발생
          const inputEvent = new Event('input', { bubbles: true, cancelable: true });
          body.dispatchEvent(inputEvent);

          showToast('메일이 업데이트되었습니다', 'success');
          return true;
        }
      } catch (e) {
        console.log('[메일 다듬기] iframe 업데이트 실패:', e.message);
      }
    }

    // contenteditable 요소 찾기
    const editables = document.querySelectorAll('[contenteditable="true"]');
    console.log('[메일 다듬기] 찾은 contenteditable 요소 개수:', editables.length);

    for (const editable of editables) {
      if (editable.innerText && editable.innerText.trim()) {
        console.log('[메일 다듬기] contenteditable 요소 발견, 업데이트 시도');
        editable.innerHTML = newContent.replace(/\n/g, '<br>');

        // 변경 이벤트 발생
        editable.dispatchEvent(new Event('input', { bubbles: true }));

        showToast('메일이 업데이트되었습니다', 'success');
        return true;
      }
    }

    // textarea 찾기
    const textareas = document.querySelectorAll('textarea');
    console.log('[메일 다듬기] 찾은 textarea 개수:', textareas.length);

    for (const textarea of textareas) {
      if (textarea.value && textarea.value.trim()) {
        console.log('[메일 다듬기] textarea 발견, 업데이트 시도');
        textarea.value = newContent;
        // input 이벤트 발생시켜 변경 감지
        textarea.dispatchEvent(new Event('input', { bubbles: true }));
        showToast('메일이 업데이트되었습니다', 'success');
        return true;
      }
    }

    console.log('[메일 다듬기] 업데이트 가능한 요소를 찾지 못함');
    showToast('메일 내용을 업데이트할 수 없습니다', 'error');
    return false;
  }

  // 토스트 메시지 표시
  function showToast(message, type = 'info') {
    // 기존 토스트 제거
    const existingToast = document.querySelector('.mail-rewrite-toast');
    if (existingToast) {
      existingToast.remove();
    }

    const toast = document.createElement('div');
    toast.className = `mail-rewrite-toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
      toast.remove();
    }, 3000);
  }

  console.log('메일 다듬기 확장프로그램이 로드되었습니다.');
})();
