import React, { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import TopLogo from '../components/TopLogo';
import './VoiceOrder.css';

const API_URL =
  process.env.REACT_APP_API_URL ||
  (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface VoiceMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

interface SummaryItem {
  name: string;
  quantity: number;
}

interface VoiceOrderSummary {
  dinnerName?: string;
  servingStyle?: string;
  deliverySlot?: string;
  deliveryAddress?: string;
  contactPhone?: string;
  specialRequests?: string;
  items: SummaryItem[];
  readyForConfirmation: boolean;
  missingFields: string[];
}

const VoiceOrderPage: React.FC = () => {
  const [sessionId, setSessionId] = useState<string>('');
  const [messages, setMessages] = useState<VoiceMessage[]>([]);
  const [summary, setSummary] = useState<VoiceOrderSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [recording, setRecording] = useState<boolean>(false);
  const [textInput, setTextInput] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [confirmation, setConfirmation] = useState<string | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const silenceStartRef = useRef<number | null>(null);

  useEffect(() => {
    startSession();
    return () => {
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
      if (audioContextRef.current) {
        audioContextRef.current.close().catch(() => undefined);
        audioContextRef.current = null;
      }
    };
  }, []);

  const authHeaders = () => {
    const token = localStorage.getItem('token');
    if (!token) throw new Error('로그인이 필요합니다.');
    return { Authorization: `Bearer ${token}` };
  };

  const startSession = async () => {
    try {
      setLoading(true);
      setError('');
      const response = await axios.post(
        `${API_URL}/voice-orders/start`,
        {},
        { headers: authHeaders() }
      );
      setSessionId(response.data.sessionId);
      setMessages(response.data.messages || []);
      setSummary(response.data.summary || null);
    } catch (err: any) {
      console.error(err);
      setError(
        err.response?.data?.error ||
          '음성 주문 세션을 시작할 수 없습니다. 잠시 후 다시 시도해주세요.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleTextSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!textInput.trim()) return;
    await sendUtterance(textInput.trim());
    setTextInput('');
  };

  const sendUtterance = async (text: string) => {
    if (!sessionId) return;
    const tempId = `local-${Date.now()}`;
    const optimisticMessage: VoiceMessage = {
      id: tempId,
      role: 'user',
      content: text,
      timestamp: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, optimisticMessage]);
    try {
      setLoading(true);
      setError('');
      const response = await axios.post(
        `${API_URL}/voice-orders/utterance`,
        {
          sessionId,
          userText: text,
        },
        { headers: authHeaders() }
      );
      setMessages((prev) => {
        const replaced = prev.map((message) =>
          message.id === tempId ? response.data.userMessage : message
        );
        return [...replaced, response.data.agentMessage];
      });
      setSummary(response.data.summary);
    } catch (err: any) {
      console.error(err);
      setMessages((prev) => prev.filter((message) => message.id !== tempId));
      setError(
        err.response?.data?.error ||
          '상담원과 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'
      );
    } finally {
      setLoading(false);
    }
  };

  const SILENCE_THRESHOLD = 0.015;
  const SILENCE_DURATION_MS = 1500;

  const cleanupAudioGraph = () => {
    silenceStartRef.current = null;
    if (analyserRef.current) {
      analyserRef.current.disconnect();
      analyserRef.current = null;
    }
    if (audioContextRef.current) {
      audioContextRef.current.close().catch(() => undefined);
      audioContextRef.current = null;
    }
  };

  const startRecording = async () => {
    if (!sessionId) return;
    try {
      setError('');
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      audioChunksRef.current = [];
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };
      recorder.onstop = () => {
        setRecording(false);
        stream.getTracks().forEach((track) => track.stop());
        const blob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        sendAudio(blob);
      };
      recorder.start();

      const audioContext = new AudioContext();
      const source = audioContext.createMediaStreamSource(stream);
      const analyser = audioContext.createAnalyser();
      analyser.fftSize = 2048;
      source.connect(analyser);
      audioContextRef.current = audioContext;
      analyserRef.current = analyser;
      const dataArray = new Uint8Array(analyser.fftSize);

      const detectSilence = () => {
        if (!mediaRecorderRef.current || mediaRecorderRef.current.state !== 'recording') {
          cleanupAudioGraph();
          return;
        }
        analyser.getByteTimeDomainData(dataArray);
        let sumSquares = 0;
        for (let i = 0; i < dataArray.length; i += 1) {
          const centered = (dataArray[i] - 128) / 128;
          sumSquares += centered * centered;
        }
        const rms = Math.sqrt(sumSquares / dataArray.length);
        if (rms < SILENCE_THRESHOLD) {
          if (silenceStartRef.current === null) {
            silenceStartRef.current = performance.now();
          } else if (performance.now() - silenceStartRef.current > SILENCE_DURATION_MS) {
            stopRecording();
            return;
          }
        } else {
          silenceStartRef.current = null;
        }
        requestAnimationFrame(detectSilence);
      };
      requestAnimationFrame(detectSilence);

      mediaRecorderRef.current = recorder;
      setRecording(true);
      setConfirmation(null);
    } catch (err) {
      console.error(err);
      setError('마이크 권한이 필요합니다. 브라우저 설정을 확인해주세요.');
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
      cleanupAudioGraph();
      mediaRecorderRef.current.stop();
    }
  };

  const sendAudio = async (blob: Blob) => {
    if (!sessionId) return;
    try {
      setLoading(true);
      setError('');
      const formData = new FormData();
      formData.append('sessionId', sessionId);
      formData.append('file', new File([blob], 'voice-command.webm', { type: blob.type }));
      const response = await axios.post(`${API_URL}/voice-orders/transcribe`, formData, {
        headers: {
          ...authHeaders(),
          'Content-Type': 'multipart/form-data',
        },
      });
      const text = response.data.text;
      await sendUtterance(text);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.error || '음성 인식에 실패했습니다.');
    } finally {
      setLoading(false);
      cleanupAudioGraph();
    }
  };

  const handleConfirm = async () => {
    if (!summary?.readyForConfirmation || !sessionId) return;
    try {
      setLoading(true);
      setError('');
      const response = await axios.post(
        `${API_URL}/voice-orders/confirm`,
        { sessionId },
        { headers: authHeaders() }
      );
      setConfirmation(response.data.confirmationMessage);
      setSummary(response.data.summary);
      setMessages((prev) => [
        ...prev,
        {
          id: `confirmation-${response.data.orderId}`,
          role: 'assistant',
          content: response.data.confirmationMessage,
          timestamp: new Date().toISOString(),
        },
      ]);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.error || '주문 확정에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const renderMessage = (message: VoiceMessage) => (
    <div key={message.id} className={`voice-message ${message.role}`}>
      <div className="bubble">{message.content}</div>
      <div className="timestamp">
        {new Date(message.timestamp).toLocaleTimeString('ko-KR', {
          hour: '2-digit',
          minute: '2-digit',
        })}
      </div>
    </div>
  );

  const renderSummaryItems = () => {
    if (!summary?.items?.length) {
      return <p className="muted">아직 구성 정보가 없습니다.</p>;
    }
    return summary.items.map((item) => (
      <div key={item.name} className="summary-item">
        <span>{item.name}</span>
        <span className="quantity">x{item.quantity}</span>
      </div>
    ));
  };

  return (
    <div className="voice-order-page">
      <TopLogo />
      <div className="voice-order-layout">
        <section className="chat-panel">
          <div className="panel-header">
            <div>
              <h2>음성 주문 상담</h2>
              <p className="muted">
                상담원과 자연스럽게 대화하며 주문을 완성하세요. (존댓말 응답)
              </p>
            </div>
            <div className="status-badges">
              <span className={`badge ${summary?.readyForConfirmation ? 'ready' : 'pending'}`}>
                {summary?.readyForConfirmation ? '주문 정보 준비 완료' : '추가 정보 필요'}
              </span>
              {loading && <span className="badge subtle">처리 중...</span>}
            </div>
          </div>

          <div className="messages-window">
            {messages.length === 0 && (
              <div className="placeholder">
                상담원이 인사를 준비 중입니다. 잠시만 기다려 주세요.
              </div>
            )}
            {messages.map(renderMessage)}
          </div>

          {error && <div className="error-banner">{error}</div>}
          {confirmation && (
            <div className="success-banner">
              {confirmation}
            </div>
          )}

          <div className="controls">
            <button
              className={`btn ${recording ? 'btn-danger' : 'btn-primary'}`}
              onClick={recording ? stopRecording : startRecording}
              disabled={!sessionId || loading}
            >
              {recording ? '■ 녹음 중지' : '🎙️ 음성 녹음'}
            </button>
            <form onSubmit={handleTextSubmit} className="text-input-form">
              <input
                type="text"
                value={textInput}
                onChange={(e) => setTextInput(e.target.value)}
                placeholder="텍스트로도 말씀하실 수 있어요."
              />
              <button type="submit" className="btn btn-secondary" disabled={!textInput.trim()}>
                전송
              </button>
            </form>
          </div>

          <div className="examples">
            <p className="muted">예시 발화: “맛있는 디너 추천해 주세요”, “샴페인 축제 디너 디럭스로 바꿀게요”, “바게트빵 6개로 늘려 주세요”</p>
          </div>
        </section>

        <aside className="summary-panel">
          <h3>주문 요약</h3>
          <div className="summary-card">
            <div className="summary-row">
              <span>디너</span>
              <strong>{summary?.dinnerName || '-'}</strong>
            </div>
            <div className="summary-row">
              <span>서빙 스타일</span>
              <strong>{summary?.servingStyle || '-'}</strong>
            </div>
            <div className="summary-row">
              <span>배달 시간</span>
              <strong>{summary?.deliverySlot || '-'}</strong>
            </div>
            <div className="summary-row">
              <span>주소</span>
              <strong>{summary?.deliveryAddress || '-'}</strong>
            </div>
            <div className="summary-row">
              <span>연락처</span>
              <strong>{summary?.contactPhone || '-'}</strong>
            </div>
            <div className="summary-section">
              <h4>구성</h4>
              {renderSummaryItems()}
            </div>
            {summary?.missingFields?.length ? (
              <div className="missing-fields">
                <h4>필요 정보</h4>
                <ul>
                  {summary.missingFields.map((field) => (
                    <li key={field}>{field}</li>
                  ))}
                </ul>
              </div>
            ) : (
              <p className="muted tiny">모든 필수 정보가 채워졌습니다.</p>
            )}
            <button
              className="btn btn-primary confirm-button"
              onClick={handleConfirm}
              disabled={!summary?.readyForConfirmation || loading || !!confirmation}
            >
              주문 확정하기
            </button>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default VoiceOrderPage;


