import { useState, useEffect , useRef , useMemo } from 'react';
import api from '../api/axios';
import { throttle } from '../utils/remoteCursor';
import RemoteCursorFlag from './RemoteCursorFlag';

// near top of file, outside the component
function getSiteId() {
  let id = sessionStorage.getItem('cb-site-id');
  if (!id) {
    id = crypto.randomUUID();
    sessionStorage.setItem('cb-site-id', id);
  }
  return id;
}

function diffStrings(oldStr, newStr) {
  let start = 0;
  while (start < oldStr.length && start < newStr.length && oldStr[start] === newStr[start]) {
    start++;
  }
  let oldEnd = oldStr.length;
  let newEnd = newStr.length;
  while (oldEnd > start && newEnd > start && oldStr[oldEnd - 1] === newStr[newEnd - 1]) {
    oldEnd--;
    newEnd--;
  }
  return {
    start,
    deletedText: oldStr.slice(start, oldEnd),
    insertedText: newStr.slice(start, newEnd),
  };
}


const avatarColorPalette = ['#E8A33D', '#6FA8DC', '#8E7CC3', '#93C47D', '#E06666', '#4DB6AC', '#F06292', '#9575CD'];
function getAvatarColor(username) {
  let hash = 0;
  for (let i = 0; i < username.length; i++) {
    hash = username.charCodeAt(i) + ((hash << 5) - hash);
  }
  return avatarColorPalette[Math.abs(hash) % avatarColorPalette.length];
}



// Mirrors RgaService.buildOrderedChars on the backend: group by afterId,
// sort concurrent siblings by siteId+seq (descending), walk the tree,
// skip deleted chars in the output but still recurse into their children
// so text after a deleted char isn't lost.
function buildOrderedChars(rawChars) {
  const childrenByParent = new Map();
  for (const c of rawChars) {
    const key = c.afterId ?? null;
    if (!childrenByParent.has(key)) childrenByParent.set(key, []);
    childrenByParent.get(key).push(c);
  }

  for (const siblings of childrenByParent.values()) {
    siblings.sort((a, b) => {
      if (a.siteId !== b.siteId) return a.siteId < b.siteId ? 1 : -1;
      return b.seq - a.seq;
    });
  }

  const result = [];
  function walk(parentId) {
    const children = childrenByParent.get(parentId ?? null);
    if (!children) return;
    for (const c of children) {
      if (!c.deleted) result.push(c);
      walk(c.charId);
    }
  }
  walk(null);
  return result;
}


// contentEditable has no selectionStart/setSelectionRange like a textarea.
// These convert between a flat character index (what our RGA logic uses)
// and a real DOM selection (a specific text node + offset within it).
function getCaretIndex(el) {
  const selection = window.getSelection();
  if (!selection || selection.rangeCount === 0) return 0;
  const range = selection.getRangeAt(0);
  if (!el.contains(range.startContainer)) return 0;

  const preRange = document.createRange();
  preRange.selectNodeContents(el);
  preRange.setEnd(range.startContainer, range.startOffset);
  return preRange.toString().length;
}

function setCaretIndex(el, index) {
  const range = document.createRange();
  const selection = window.getSelection();
  let remaining = index;
  let node = null;
  let offset = 0;

  const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null);
  let current;
  while ((current = walker.nextNode())) {
    const len = current.textContent.length;
    if (remaining <= len) {
      node = current;
      offset = remaining;
      break;
    }
    remaining -= len;
  }

  if (!node) {
    node = el;
    offset = el.childNodes.length;
  }

  range.setStart(node, offset);
  range.collapse(true);
  selection.removeAllRanges();
  selection.addRange(range);
}



function CardModal({card, listId , wsCharQueue , wsConnected , onClose  , onTitleSaved , stompClient , boardId , remoteDescriptionCursor }) {
  const [title, setTitle] = useState(card.title);
  const [rawChars, setRawChars] = useState([]);
  const [loading, setLoading] = useState(true);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const seqRef = useRef(0);
  const siteId = useState(() => getSiteId())[0];
  const pendingOpsRef = useRef([]);
  const prevConnectedRef = useRef(wsConnected);
  const textareaRef = useRef(null);
  const pendingCursorRestoreRef = useRef(null);
  const descriptionContainerRef = useRef(null);
  const [remoteFlagPos, setRemoteFlagPos] = useState(null);
  const myUsername = localStorage.getItem('username');


  const chars = useMemo(() => buildOrderedChars(rawChars), [rawChars]);
  const description = chars.map((c) => c.value).join('');

  useEffect(() => {
    fetchChars();
  }, [card.id]);

    const fetchChars = async () => {
    try {
      const response = await api.get(`/cards/${card.id}/characters`);
      setRawChars(response.data);
    } catch (err) {
      setRawChars([]);
    } finally {
      setLoading(false);
    }
  };

  // ADD THIS ↓↓↓
  const flushPendingOps = async () => {
    
    if (pendingOpsRef.current.length === 0) return;
    await fetchChars();
    const ops = [...pendingOpsRef.current].sort((a, b) => a.seq - b.seq);
    pendingOpsRef.current = [];
    for (const op of ops) {
       
           try {
        if (op.type === 'delete') {
          await api.delete(`/cards/${card.id}/characters/${op.charId}`);
        } else {
          await api.post(`/cards/${card.id}/characters`, op.char);
        }
      } catch (err) {
        if (!err.response) {
          pendingOpsRef.current.push(op);
        } else {
          console.error('Retry failed with real error:', err.response.status, op);
        }
      }
    }
  };

  useEffect(() => {
    
    if (!prevConnectedRef.current && wsConnected) {
      flushPendingOps();
    }
    prevConnectedRef.current = wsConnected;
  }, [wsConnected]);
  // ADD THIS ↑↑↑



  const processedCountRef = useRef(0);

      useEffect(() => {
        processedCountRef.current = 0;
      }, [card.id]);

      useEffect(() => {
      const handleOnline = () => {
        
        flushPendingOps();
      };
      window.addEventListener('online', handleOnline);
      return () => window.removeEventListener('online', handleOnline);
    }, []);


  useEffect(() => {
    if (!wsCharQueue) return;
    
    const newMessages = wsCharQueue.slice(processedCountRef.current);
    processedCountRef.current = wsCharQueue.length;

    const relevant = newMessages.filter((m) => m.cardId === card.id);
    if (relevant.length === 0) return;

    // Ignore our own echoed-back messages for cursor purposes — we already
   // handled our own cursor correctly when we typed locally. Only a
   // genuinely remote edit (different siteId) should trigger a restore.
   const remoteMessages = relevant.filter((m) => m.siteId !== siteId);

          // If the user is actively focused in the textarea, remember which
      // character their cursor is sitting right after, so we can find that
      // same character again after the remote update and put the cursor
      // back next to it (instead of letting the browser dump it at the end).
      if (remoteMessages.length > 0 && document.activeElement === textareaRef.current) {
        const pos = getCaretIndex(textareaRef.current);
        const anchorCharId = pos > 0 ? chars[pos - 1]?.charId ?? null : null;
        pendingCursorRestoreRef.current = { anchorCharId, hadNoAnchor: pos === 0 };
         
      }

    setRawChars((prev) => {
      let next = prev;
      for (const msg of relevant) {
        if (msg.type === 'CHAR_INSERTED') {
          if (next.some((c) => c.charId === msg.charId)) continue;
          next = [...next, {
            charId: msg.charId,
            value: msg.value,
            afterId: msg.afterId,
            siteId: msg.siteId,
            seq: msg.seq,
            deleted: false,
          }];
        } else if (msg.type === 'CHAR_DELETED') {
          next = next.map((c) => (c.charId === msg.charId ? { ...c, deleted: true } : c));
        }
      }
      return next;
    });
  }, [wsCharQueue]);


          // Runs after every re-render caused by a chars change. If we stashed a
    // pending cursor restore (see above), find where that same character
    // ended up in the new text and put the cursor right after it.

    // contentEditable isn't a controlled element like <textarea value=...>,
    // so we manually push `description` into the DOM when it changes. The
    // equality check is important — without it we'd reset the DOM (and
    // scatter the cursor) even when nothing actually changed.
    useEffect(() => {
      const el = textareaRef.current;
      if (!el) return;
      if (el.textContent !== description) {
        el.textContent = description;
      }
    }, [chars, drawerOpen]);


    useEffect(() => {
      const pending = pendingCursorRestoreRef.current;
      if (!pending || !textareaRef.current) return;
      pendingCursorRestoreRef.current = null;

      let newPos;
      if (pending.hadNoAnchor) {
        newPos = 0;
      } else {
        const idx = chars.findIndex((c) => c.charId === pending.anchorCharId);
        // If the anchor char got deleted concurrently, idx will be -1 —
        // fall back to end of text rather than guessing.
        newPos = idx >= 0 ? idx + 1 : chars.length;
      }
      
      setCaretIndex(textareaRef.current, newPos);
    }, [chars]);


      // Positions the remote cursor flag by finding where anchorCharId landed
  // in the current text, then asking the browser for that character's
  // actual pixel position (getClientRects handles line-wrapping for us —
  // this is exactly why title-field pixel measuring wouldn't work here).
  useEffect(() => {
    if (!remoteDescriptionCursor || !textareaRef.current || !drawerOpen) {
      setRemoteFlagPos(null);
      return;
    }
    const idx = chars.findIndex((c) => c.charId === remoteDescriptionCursor.anchorCharId);
    const charIndex = idx >= 0 ? idx + 1 : (remoteDescriptionCursor.anchorCharId === null ? 0 : chars.length);

    const el = textareaRef.current;
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null);
    let remaining = charIndex;
    let node = null;
    let offset = 0;
    let current;
    while ((current = walker.nextNode())) {
      const len = current.textContent.length;
      if (remaining <= len) {
        node = current;
        offset = remaining;
        break;
      }
      remaining -= len;
    }
    if (!node) {
      setRemoteFlagPos(null);
      return;
    }

    const range = document.createRange();
    range.setStart(node, offset);
    range.collapse(true);
    const rect = range.getClientRects()[0];
    const containerRect = descriptionContainerRef.current.getBoundingClientRect();
    if (!rect) {
      setRemoteFlagPos(null);
      return;
    }

    setRemoteFlagPos({
      top: rect.top - containerRect.top,
      left: rect.left - containerRect.left,
    });
  }, [remoteDescriptionCursor, chars, drawerOpen]);


    const handleDescriptionChange = async (e) => {
     const newValue = e.currentTarget.textContent ; 
    const oldValue = description;
    const { start, deletedText, insertedText } = diffStrings(oldValue, newValue);

    const deletedChars = chars.slice(start, start + deletedText.length);
    const beforeChars = chars.slice(0, start);
    const afterChars = chars.slice(start + deletedText.length);

    let prevCharId = beforeChars.length > 0 ? beforeChars[beforeChars.length - 1].charId : null;
    const insertedChars = [];
    for (const ch of insertedText) {
      const newChar = {
        charId: crypto.randomUUID(),
        value: ch,
        afterId: prevCharId,
        siteId,
        seq: seqRef.current++,
        deleted: false,
      };
      insertedChars.push(newChar);
      prevCharId = newChar.charId;
    }


    // Remember where OUR OWN cursor should land after this edit, anchored
    // to a stable charId (last char we just typed, or the char right
    // before our edit point if we only deleted). Without this, the DOM
    // sync effect below can reset textContent and silently drop the
    // caret to wherever the browser defaults — which is the bug you hit.
    const anchorCharId = insertedChars.length > 0
      ? insertedChars[insertedChars.length - 1].charId
      : (beforeChars.length > 0 ? beforeChars[beforeChars.length - 1].charId : null);
    pendingCursorRestoreRef.current = { anchorCharId, hadNoAnchor: anchorCharId === null };



        setRawChars((prev) => {
      const deletedIds = new Set(deletedChars.map((c) => c.charId));
      const updated = prev.map((c) =>
        deletedIds.has(c.charId) ? { ...c, deleted: true } : c
      );
      return [...updated, ...insertedChars];
    });

                if (deletedChars.length > 0) {
        try {
          await api.delete(`/cards/${card.id}/characters/batch`, {
            data: deletedChars.map((dc) => dc.charId),
          });
        } catch (err) {
          if (!err.response) {
            for (const dc of deletedChars) {
              pendingOpsRef.current.push({ type: 'delete', charId: dc.charId, seq: dc.seq });
            }
          } else {
            console.error('Delete failed:', err.response.status, err.response.data);
          }
        }
      }
      if (insertedChars.length > 0) {
        try {
          await api.post(`/cards/${card.id}/characters/batch`, insertedChars);
        } catch (err) {
          if (!err.response) {
            for (const ic of insertedChars) {
              pendingOpsRef.current.push({ type: 'insert', char: ic, seq: ic.seq });
            }
          } else {
            console.error('Insert failed:', err.response.status, err.response.data);
          }
        }
      }
    };


      // contentEditable's default Enter inserts a <div> or <br>, which would
  // break our flat-string model. Force it to insert a plain '\n' instead,
  // which then flows through handleDescriptionChange like any other char.
  const handleDescriptionKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      document.execCommand('insertText', false, '\n');
    }
  };

  // Force paste to insert as plain text — otherwise pasting from a rich
  // source (Word, a webpage) injects formatted HTML nodes that break the
  // flat text-node structure our caret math relies on.
  const handleDescriptionPaste = (e) => {
    e.preventDefault();
    const text = e.clipboardData.getData('text/plain');
    document.execCommand('insertText', false, text);
  };


    const sendCursorUpdate = (anchorCharId) => {
    if (!stompClient?.connected) return;
    stompClient.publish({
      destination: '/app/cursor',
      body: JSON.stringify({
        boardId,
        fieldType: 'CARD_DESCRIPTION',
        entityId: card.id,
        charIndex: null,
        anchorCharId,
      }),
    });
  };
  const throttledSendCursorUpdateRef = useRef(throttle(sendCursorUpdate, 300));

  const sendCursorClear = () => {
    if (!stompClient?.connected) return;
    stompClient.publish({
      destination: '/app/cursor/clear',
      body: JSON.stringify({ boardId, fieldType: 'CARD_DESCRIPTION', entityId: card.id }),
    });
  };

  // Same anchorCharId logic already used for local cursor-restore after a
  // remote edit — reused here so "where is my cursor" is computed the
  // exact same way whether it's for restoring position or broadcasting it.
  const getCurrentAnchorCharId = () => {
    if (!textareaRef.current) return null;
    const pos = getCaretIndex(textareaRef.current);
    return pos > 0 ? chars[pos - 1]?.charId ?? null : null;
  };


    useEffect(() => {
        if (drawerOpen && textareaRef.current) {
          textareaRef.current.focus();
        }
      }, [drawerOpen]);

  const handleTitleBlur = async () => {
    if (title === card.title) return;
    try {
      await api.put(`/lists/${listId}/cards/${card.id}`, {
        title,
        description: card.description,
        position: card.position,
      });
      onTitleSaved(card.id, title);
    } catch (err) {
      setTitle(card.title);
    }
  };

    return (
    <>
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
          <input
            className="card-modal-title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onBlur={handleTitleBlur}
          />

          <div className="card-modal-description-label">
            <span>Description</span>
            <button className="ghost description-expand-btn" onClick={() => setDrawerOpen(true)}>
              Expand
            </button>
          </div>
          {loading ? (
            <p>Loading...</p>
          ) : (
            <div className="card-modal-description-preview" onClick={() => setDrawerOpen(true)}>
              {description || <span className="text-dim">Click to add a description...</span>}
            </div>
          )}

          <button className="ghost" onClick={onClose}>Close</button>
        </div>
      </div>

      {drawerOpen && (
        <div className="description-drawer-overlay" onClick={() => setDrawerOpen(false)}>
          <div className="description-drawer" onClick={(e) => e.stopPropagation()}>
            <div className="description-drawer-header">
              <span>Description</span>
              <button className="ghost" onClick={() => setDrawerOpen(false)}>Done</button>
            </div>
            {loading ? (
              <p>Loading...</p>
            ) : (
               <div ref={descriptionContainerRef} className="description-editor-wrapper">
                <div
                  ref={textareaRef}
                  className="card-modal-description drawer-textarea"
                  contentEditable
                  suppressContentEditableWarning
                  onInput={handleDescriptionChange}
                  onKeyDown={handleDescriptionKeyDown}
                  onPaste={handleDescriptionPaste}
                  onFocus={() => sendCursorUpdate(getCurrentAnchorCharId())}
                  onBlur={sendCursorClear}
                  onKeyUp={() => throttledSendCursorUpdateRef.current(getCurrentAnchorCharId())}
                  onClick={() => throttledSendCursorUpdateRef.current(getCurrentAnchorCharId())}
                />
                {remoteFlagPos && remoteDescriptionCursor && remoteDescriptionCursor.username !== myUsername && (
                  <div style={{ position: 'absolute', top: remoteFlagPos.top, left: remoteFlagPos.left }}>
                    <RemoteCursorFlag
                      username={remoteDescriptionCursor.username}
                      color={getAvatarColor(remoteDescriptionCursor.username)}
                      left={0}
                    />
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}

export default CardModal;