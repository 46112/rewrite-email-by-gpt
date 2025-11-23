document.addEventListener('DOMContentLoaded', async () => {
  const statusEl = document.getElementById('status');
  const rewriteBtn = document.getElementById('rewriteBtn');
  const recipientSelect = document.getElementById('recipient');
  const toneSelect = document.getElementById('tone');
  const resultContainer = document.getElementById('resultContainer');
  const suggestionsList = document.getElementById('suggestions');
  const serverUrlInput = document.getElementById('serverUrl');
  const saveSettingsBtn = document.getElementById('saveSettings');

  // 저장된 설정 불러오기
  const settings = await chrome.storage.local.get(['serverUrl', 'recipient', 'tone']);
  if (settings.serverUrl) {
    serverUrlInput.value = settings.serverUrl;
  }
  if (settings.recipient) {
    recipientSelect.value = settings.recipient;
  }
  if (settings.tone) {
    toneSelect.value = settings.tone;
  }

  // 현재 탭이 네이버 메일인지 확인
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  const isNaverMail = tab.url && tab.url.includes('mail.naver.com');

  if (isNaverMail) {
    statusEl.textContent = '메일 내용을 다듬을 준비가 되었습니다';
    statusEl.className = 'status success';
    rewriteBtn.disabled = false;
  } else {
    statusEl.textContent = '네이버 메일 페이지에서 사용해주세요';
    statusEl.className = 'status';
    rewriteBtn.disabled = true;
  }

  // 설정 저장
  saveSettingsBtn.addEventListener('click', async () => {
    await chrome.storage.local.set({
      serverUrl: serverUrlInput.value
    });
    showToast('설정이 저장되었습니다');
  });

  // 옵션 변경시 저장
  recipientSelect.addEventListener('change', async () => {
    await chrome.storage.local.set({ recipient: recipientSelect.value });
  });

  toneSelect.addEventListener('change', async () => {
    await chrome.storage.local.set({ tone: toneSelect.value });
  });

  // 메일 다듬기 버튼 클릭
  rewriteBtn.addEventListener('click', async () => {
    try {
      rewriteBtn.disabled = true;
      rewriteBtn.textContent = '처리 중...';
      statusEl.textContent = '메일 내용을 분석하고 있습니다...';
      statusEl.className = 'status loading';
      resultContainer.style.display = 'none';

      // content script에 메시지 전송하여 메일 내용 가져오기
      const response = await chrome.tabs.sendMessage(tab.id, {
        action: 'getMailContent'
      });

      if (!response || !response.content) {
        throw new Error('메일 내용을 가져올 수 없습니다. 메일 작성 화면인지 확인해주세요.');
      }

      // 서버로 요청
      const serverUrl = serverUrlInput.value || 'http://localhost:8080';
      const apiResponse = await fetch(`${serverUrl}/api/mail/rewrite`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          content: response.content,
          recipient: recipientSelect.value,
          tone: toneSelect.value
        })
      });

      if (!apiResponse.ok) {
        throw new Error(`서버 오류: ${apiResponse.status}`);
      }

      const result = await apiResponse.json();

      // content script에 결과 전송하여 메일 내용 업데이트
      await chrome.tabs.sendMessage(tab.id, {
        action: 'updateMailContent',
        content: result.rewrittenContent
      });

      // 개선 사항 표시
      if (result.suggestions && result.suggestions.length > 0) {
        suggestionsList.innerHTML = '';
        result.suggestions.forEach(suggestion => {
          const li = document.createElement('li');
          li.textContent = suggestion;
          suggestionsList.appendChild(li);
        });
        resultContainer.style.display = 'block';
      }

      statusEl.textContent = '메일이 성공적으로 다듬어졌습니다!';
      statusEl.className = 'status success';

    } catch (error) {
      console.error('Error:', error);
      statusEl.textContent = error.message || '오류가 발생했습니다';
      statusEl.className = 'status error';
    } finally {
      rewriteBtn.disabled = false;
      rewriteBtn.textContent = '메일 다듬기';
    }
  });

  function showToast(message) {
    const toast = document.createElement('div');
    toast.textContent = message;
    toast.style.cssText = `
      position: fixed;
      bottom: 10px;
      left: 50%;
      transform: translateX(-50%);
      background: #333;
      color: white;
      padding: 8px 16px;
      border-radius: 4px;
      font-size: 12px;
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2000);
  }
});
