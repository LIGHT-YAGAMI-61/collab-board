import { useState, useEffect  , useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useDroppable } from '@dnd-kit/core';
import api from '../api/axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import CardModal from '../components/CardModal';
import RemoteCursorFlag from '../components/RemoteCursorFlag';
import { measureCaretOffset, throttle } from '../utils/remoteCursor';



    const avatarColorPalette = ['#E8A33D', '#6FA8DC', '#8E7CC3', '#93C47D', '#E06666', '#4DB6AC', '#F06292', '#9575CD'];

    function getAvatarColor(username) {
      let hash = 0;  for (let i = 0; i < username.length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
      }
      const index = Math.abs(hash) % avatarColorPalette.length;
      return avatarColorPalette[index];
    }

    function getInitials(username) {
      return username.slice(0, 2).toUpperCase();
    }




function Card({ card, listId, handleDeleteCard, onOpen, isAdmin, titleValue, onTitleChange, onTitleFocus, onTitleBlur, titleInputRefs, remoteCursor, onCursorMove, onCursorFocus, onCursorClear }) {
  const { attributes, listeners, setNodeRef, transform, transition } = useSortable({
    id: `card-${card.id}`,
    data: { type: 'card', card },
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div  ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      className="kanban-card"
      onClick={() => onOpen({ ...card , listId })} 
      >
      <div>
         <div
          className="cursor-field-wrapper"
          onClick={(e) => e.stopPropagation()}
          onPointerDown={(e) => e.stopPropagation()}
        >
          <input
            ref={(el) => { titleInputRefs.current[card.id] = el; }}
            className="card-title card-title-input"
            value={titleValue}
            onChange={(e) => {
              onTitleChange(card.id, e.target.value);
              onCursorMove(e.target.selectionStart);
            }}
            onFocus={(e) => {
              onTitleFocus(card.id);
              onCursorFocus(e.target.selectionStart);
            }}
            onBlur={() => {
              onTitleBlur(listId, card.id);
              onCursorClear();
            }}
          />
          {remoteCursor && (
            <RemoteCursorFlag
              username={remoteCursor.username}
              color={getAvatarColor(remoteCursor.username)}
              left={measureCaretOffset(titleInputRefs.current[card.id], remoteCursor.charIndex)}
            />
          )}
        </div>
        <div className="card-id">#{card.id}</div>
      </div>
      
        {isAdmin && (
        <button
          className="card-delete"
          onClick={(e) => {
            e.stopPropagation();
            handleDeleteCard(listId, card.id);
          }}
        >
          ×
        </button>
      )}
    </div>
  );
}


const listColorPalette = ['#E8A33D', '#6FA8DC', '#8E7CC3', '#93C47D', '#E06666'];

function ListColumn({ list, index, titleValue, onTitleChange, onTitleFocus, onTitleBlur, titleInputRefs, remoteCursor, onCursorMove, onCursorFocus, onCursorClear, newCardTitles, setNewCardTitles, handleCreateCard, handleDeleteList, handleDeleteCard  , onOpenCard , isAdmin, cardTitleInputs, cardTitleInputRefs, remoteCursors, onCardTitleChange, onCardTitleFocus, onCardTitleBlur, onCardCursorMove, onCardCursorFocus, onCardCursorClear }) {
  const cardIds = list.cards?.map((c) => `card-${c.id}`) || [];
  const { setNodeRef } = useDroppable({
    id: `list-${list.id}`,
    data: { type: 'list', listId: list.id },
  });
  const color = listColorPalette[index % listColorPalette.length];

  return (
    <div className="list-column" style={{ borderLeftColor: color }}>
      <div className="list-column-header">
           <div className="cursor-field-wrapper">
            <input
              ref={(el) => { titleInputRefs.current[list.id] = el; }}
              className="list-title-input"
              value={titleValue}
              onChange={(e) => {
                onTitleChange(list.id, e.target.value);
                onCursorMove(e.target.selectionStart);
              }}
              onFocus={(e) => {
                onTitleFocus(list.id);
                onCursorFocus(e.target.selectionStart);
              }}
              onBlur={() => {
                onTitleBlur(list.id);
                onCursorClear();
              }}
            />
            {remoteCursor && (
              <RemoteCursorFlag
                username={remoteCursor.username}
                color={getAvatarColor(remoteCursor.username)}
                left={measureCaretOffset(titleInputRefs.current[list.id], remoteCursor.charIndex)}
              />
            )}
          </div>


        <button className="danger" onClick={() => handleDeleteList(list.id)}>Delete</button>
      </div>

      <SortableContext items={cardIds} strategy={verticalListSortingStrategy}>
        <div ref={setNodeRef} className="card-stack">
          {list.cards?.map((card) => (
            <Card
              key={card.id}
              card={card}
              listId={list.id}
              handleDeleteCard={handleDeleteCard}
              onOpen={onOpenCard}
              isAdmin={isAdmin}
              titleValue={cardTitleInputs[card.id] ?? card.title}
              onTitleChange={onCardTitleChange}
              onTitleFocus={onCardTitleFocus}
              onTitleBlur={onCardTitleBlur}
              titleInputRefs={cardTitleInputRefs}
              remoteCursor={remoteCursors[`CARD_TITLE:${card.id}`]}
              onCursorMove={(charIndex) => onCardCursorMove(card.id, charIndex)}
              onCursorFocus={(charIndex) => onCardCursorFocus(card.id, charIndex)}
              onCursorClear={() => onCardCursorClear(card.id)}
            />
          ))}
        </div>
      </SortableContext>

      <form onSubmit={(e) => handleCreateCard(e, list.id)} className="new-card-form">
        <input
          type="text"
          placeholder="New card"
          value={newCardTitles[list.id] || ''}
          onChange={(e) => setNewCardTitles({ ...newCardTitles, [list.id]: e.target.value })}
        />
        <button type="submit">Add Card</button>
      </form>
    </div>
  );
}


function BoardDetail() {
  const { boardId } = useParams();
  const [board, setBoard] = useState(null);
  const [newListTitle, setNewListTitle] = useState('');
  const [newCardTitles, setNewCardTitles] = useState({});
  const [error, setError] = useState('');
  const [members, setMembers] = useState([]);
  const [pendingRequests, setPendingRequests] = useState([]);
  const [isAdmin, setIsAdmin] = useState(false);
  const [showMembers, setShowMembers] = useState(false);
    const [wsConnected, setWsConnected] = useState(false);
    const [openCard, setOpenCard] = useState(null);
    const [charQueue, setCharQueue] = useState([]);
    const [notification, setNotification] = useState('');
    const [boardTitleInput, setBoardTitleInput] = useState('');
    const [listTitleInputs, setListTitleInputs] = useState({});
    const [cardTitleInputs, setCardTitleInputs] = useState({});
    const [activeUsers, setActiveUsers] = useState([]);
    const [remoteCursors, setRemoteCursors] = useState({});
    const boardTitleFocusedRef = useRef(false);
    const listTitleFocusedRef = useRef({});
    const listTitleInputRefs = useRef({});
    const cardTitleInputRefs = useRef({});
    const cardTitleFocusedRef = useRef({});
    const clientRef = useRef(null);
    const boardTitleInputRef = useRef(null);
    const myUsername = localStorage.getItem('username');
  const sensors = useSensors(useSensor(PointerSensor));

  useEffect(() => {
    fetchBoard();
    fetchMembers() ; 
  }, [boardId]);


      useEffect(() => {
        if (board && !boardTitleFocusedRef.current) {
          setBoardTitleInput(board.title);
        }
      }, [board?.title]);

      useEffect(() => {
        if (!board?.lists) return;
        setListTitleInputs((prev) => {
          const next = { ...prev };
          for (const l of board.lists) {
            if (!listTitleFocusedRef.current[l.id]) {
              next[l.id] = l.title;
            }
          }
          return next;
        });
      }, [board?.lists]);

       useEffect(() => {
        if (!board?.lists) return;
        setCardTitleInputs((prev) => {
          const next = { ...prev };
          for (const l of board.lists) {
            for (const c of (l.cards || [])) {
              if (!cardTitleFocusedRef.current[c.id]) {
                next[c.id] = c.title;
              }
            }
          }
          return next;
        });
      }, [board?.lists]);


      useEffect(() => {

    const token = localStorage.getItem('token');

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,

      debug: (str) => console.log('[STOMP]', str),

            onConnect: () => {
        console.log('WS connected');
        setWsConnected(true);
        client.subscribe(`/topic/board/${boardId}`, (message) => {
          handleWsMessage(message);
        });
         client.subscribe(`/topic/board/${boardId}/cursors`, (message) => {
          handleCursorMessage(message);
        });
      },
      onDisconnect: () => {
        console.log('WS disconnected');
        setWsConnected(false);
      },
      onWebSocketClose: () => {
        setWsConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
        setWsConnected(false);
      },
    });
    client.activate();
     clientRef.current = client;

    return () => client.deactivate();
  }, [boardId]);

    useEffect(() => {
    if (isAdmin) {
      fetchPendingRequests();
    }
  }, [isAdmin, boardId]);

  const fetchBoard = async () => {
    try {
      const response = await api.get(`/boards/${boardId}`);
      setBoard(response.data);
    } catch (err) {
      setError('Failed to load board.');
    }
  };
  

    const handleWsMessage = (message) => {
          
    let data;
    try {
      data = JSON.parse(message.body);
          if (data.type === 'CHAR_INSERTED' || data.type === 'CHAR_DELETED') {
      setCharQueue((prev) => [...prev , data]) ; 
      return;
    }
    if (data.type === 'PRESENCE_UPDATE') {
          setActiveUsers(data.activeUsers || []);
          return;
        }
    } catch (err) {
      fetchBoard();
      return;
    }

    setBoard((prev) => {
      if (!prev) return prev;

      switch (data.type) {
        case 'CARD_CREATED': {
          const targetListId = data.listId;
          return {
            ...prev,
            lists: prev.lists.map((l) =>
              l.id === targetListId
               ? { ...l, cards: [...(l.cards || []), data.card] }
                : l
            ),
          };
        }

        case 'CARD_UPDATED': {
          return {
            ...prev,
            lists: prev.lists.map((l) => ({
              ...l,
              cards: l.cards?.map((c) => (c.id === data.card.id ? data.card : c)),
            })),
          };
        }

        case 'CARD_DELETED': {
          setNotification(`Card deleted by ${data.deletedBy}`);
          setTimeout(() => setNotification(''), 4000);
          return {
            ...prev,
            lists: prev.lists.map((l) => ({
              ...l,
              cards: l.cards?.filter((c) => c.id !== data.cardId),
            })),
          };
        }

        case 'LIST_CREATED': {
          return {
            ...prev,
            lists: [...prev.lists, { ...data.list, cards: [] }],
          };
        }

        case 'LIST_UPDATED': {
          return {
            ...prev,
            lists: prev.lists.map((l) =>
              l.id === data.list.id ? { ...l, title: data.list.title, position: data.list.position } : l
            ),
          };
        }

        case 'LIST_DELETED': {
          return {
            ...prev,
            lists: prev.lists.filter((l) => l.id !== data.deletedListId),
          };
        }

        case 'BOARD_UPDATED': {
          return {
            ...prev,
            title: data.board.title,
          };
        }

       

        default:
          // CARD_MOVED and any unrecognized type: fall back to full refetch
          fetchBoard();
          return prev;
      }
    });
  };


   const handleCursorMessage = (message) => {
      let data;
      try {
        data = JSON.parse(message.body);
      } catch (err) {
        return;
      }
      if (data.username === myUsername) return; // ignore our own broadcasts

      const key = `${data.fieldType}:${data.entityId}`;
      if (data.type === 'CURSOR_CLEARED') {
        setRemoteCursors((prev) => {
          const next = { ...prev };
          delete next[key];
          return next;
        });
        return;
      }
      if (data.type === 'CURSOR_UPDATE') {
        setRemoteCursors((prev) => ({
          ...prev,
          [key]: {
            username: data.username,
            charIndex: data.charIndex,
            anchorCharId: data.anchorCharId,
            lastSeen: Date.now(),
          },        }));
      }
    };

    // Safety net: if a CURSOR_CLEARED never arrives (dropped connection,
    // browser crash), drop any cursor we haven't heard from in 10s.
    useEffect(() => {
      const interval = setInterval(() => {
        setRemoteCursors((prev) => {
          const now = Date.now();
          const next = {};
          for (const [key, val] of Object.entries(prev)) {
            if (now - val.lastSeen < 10000) next[key] = val;
          }
          return next;
        });
      }, 3000);
      return () => clearInterval(interval);
    }, []);

    const sendCursorUpdate = (fieldType, entityId, charIndex, anchorCharId = null) => {
      if (!clientRef.current?.connected) return;
      clientRef.current.publish({
        destination: '/app/cursor',
        body: JSON.stringify({ boardId: Number(boardId), fieldType, entityId, charIndex, anchorCharId }),
      });
    };
   const throttledSendCursorUpdateRef = useRef(throttle(sendCursorUpdate, 300));

    const sendCursorClear = (fieldType, entityId) => {
      if (!clientRef.current?.connected) return;
      clientRef.current.publish({
        destination: '/app/cursor/clear',
        body: JSON.stringify({ boardId: Number(boardId), fieldType, entityId }),
      });
    };




    const fetchMembers = async () => {
    try {
      const response = await api.get(`/boards/${boardId}/members`);
      setMembers(response.data);
      const username = localStorage.getItem('username');
      const me = response.data.find((m) => m.user.username === username);
      setIsAdmin(me?.role === 'ADMIN');
    } catch (err) {
      console.error('Failed to load members');
    }
  };

  const fetchPendingRequests = async () => {
    try {
      const response = await api.get(`/boards/${boardId}/join-requests`);
      setPendingRequests(response.data);
    } catch (err) {
      console.error('Failed to load join requests');
    }
  };

  const handleAcceptRequest = async (requestId) => {
    try {
      await api.put(`/boards/${boardId}/join-requests/${requestId}/accept`);
      fetchPendingRequests();
      fetchMembers();
    } catch (err) {
      setError('Failed to accept request.');
    }
  };

  const handleRejectRequest = async (requestId) => {
    try {
      await api.put(`/boards/${boardId}/join-requests/${requestId}/reject`);
      fetchPendingRequests();
    } catch (err) {
      setError('Failed to reject request.');
    }
  };

  
        const handleBoardTitleChange = (value) => setBoardTitleInput(value);

      const handleBoardTitleFocus = () => {
        boardTitleFocusedRef.current = true;
      };

      const handleBoardTitleBlur = async () => {
        boardTitleFocusedRef.current = false;
        if (boardTitleInput === board.title) return;
        try {
          const response = await api.put(`/boards/${boardId}`, { title: boardTitleInput });
          setBoard((prev) => ({ ...prev, title: response.data.title }));
        } catch (err) {
          setBoardTitleInput(board.title);
          setError('Failed to update board title.');
        }
      };


      const handleListTitleChange = (listId, value) => {
        setListTitleInputs((prev) => ({ ...prev, [listId]: value }));
      };

      const handleListTitleFocus = (listId) => {
        listTitleFocusedRef.current[listId] = true;
      };

      const handleListTitleBlur = async (listId) => {
        listTitleFocusedRef.current[listId] = false;
        const list = board.lists.find((l) => l.id === listId);
        const newTitle = listTitleInputs[listId];
        if (!list || newTitle === list.title) return;
        try {
          const response = await api.put(`/boards/${boardId}/lists/${listId}`, {
            title: newTitle,
            position: list.position,
          });
          setBoard((prev) => ({
            ...prev,
            lists: prev.lists.map((l) => (l.id === listId ? { ...l, title: response.data.title } : l)),
          }));
        } catch (err) {
          setListTitleInputs((prev) => ({ ...prev, [listId]: list.title }));
          setError('Failed to update list title.');
        }
      };


       const handleCardTitleChange = (cardId, value) => {
        setCardTitleInputs((prev) => ({ ...prev, [cardId]: value }));
      };

      const handleCardTitleFocus = (cardId) => {
        cardTitleFocusedRef.current[cardId] = true;
      };

      const handleCardTitleBlur = async (listId, cardId) => {
        cardTitleFocusedRef.current[cardId] = false;
        const list = board.lists.find((l) => l.id === listId);
        const card = list?.cards?.find((c) => c.id === cardId);
        const newTitle = cardTitleInputs[cardId];
        if (!card || newTitle === card.title) return;
        try {
          const response = await api.put(`/lists/${listId}/cards/${cardId}`, {
            title: newTitle,
            description: card.description,
            position: card.position,
          });
          setBoard((prev) => ({
            ...prev,
            lists: prev.lists.map((l) => ({
              ...l,
              cards: l.cards?.map((c) => (c.id === cardId ? { ...c, title: response.data.title } : c)),
            })),
          }));
          setOpenCard((oc) => (oc && oc.id === cardId ? { ...oc, title: response.data.title } : oc));
        } catch (err) {
          setCardTitleInputs((prev) => ({ ...prev, [cardId]: card.title }));
          setError('Failed to update card title.');
        }
      };


  const handleCreateList = async (e) => {
    e.preventDefault();
    if (!newListTitle.trim()) return;

    try {
      await api.post(`/boards/${boardId}/lists`, {
        title: newListTitle,
        position: (board.lists?.length || 0) + 1,
      });
      setNewListTitle('');
      fetchBoard();
    } catch (err) {
      setError('Failed to create list.');
    }
  };


  const handleDeleteList = async (listId) => {
  if (!window.confirm('Delete this list and all its cards?')) return;
  try {
    await api.delete(`/boards/${boardId}/lists/${listId}`);
    fetchBoard();
  } catch (err) {
    setError('Failed to delete list.');
  }
};

const handleDeleteCard = async (listId, cardId) => {
  if (!window.confirm('Delete this card?')) return;
  try {
    await api.delete(`/lists/${listId}/cards/${cardId}`);
    fetchBoard();
  } catch (err) {
      if (err.response?.status === 403) {
        setError('Only board admins can delete cards.');
      } else if (err.response?.status === 404) {
        setError('Card not found — it may have already been deleted.');
      } else {
        setError('Failed to delete card.');
      }
  }
};



  const handleCreateCard = async (e, listId) => {
    e.preventDefault();
    const title = newCardTitles[listId];
    if (!title || !title.trim()) return;

    const list = board.lists.find((l) => l.id === listId);

    try {
      await api.post(`/lists/${listId}/cards`, {
        title: title,
        description: '',
        position: (list.cards?.length || 0) + 1,
      });
      setNewCardTitles({ ...newCardTitles, [listId]: '' });
      fetchBoard();
    } catch (err) {
      setError('Failed to create card.');
    }
  };

  const findCardAndList = (cardDndId) => {
    const cardId = parseInt(cardDndId.replace('card-', ''));
    for (const list of board.lists) {
      const card = list.cards?.find((c) => c.id === cardId);
      if (card) return { card, list };
    }
    return null;
  };

 const handleDragEnd = async (event) => {
  const { active, over } = event;
  if (!over) return;

  const source = findCardAndList(active.id);
  if (!source) return;

  let targetListId;
  let targetPosition;

  if (over.id.toString().startsWith('card-')) {
    const target = findCardAndList(over.id);
    if (!target) return;
    targetListId = target.list.id;
    targetPosition = target.card.position;
  } else if (over.id.toString().startsWith('list-')) {
    targetListId = parseInt(over.id.replace('list-', ''));
    const targetList = board.lists.find((l) => l.id === targetListId);
    targetPosition = (targetList.cards?.length || 0) + 1;
  } else {
    return;
  }

  if (targetListId === source.list.id && targetPosition === source.card.position) return;

  try {
    await api.put(`/lists/${source.list.id}/cards/${source.card.id}/move`, {
      targetListId: targetListId,
      targetPosition: targetPosition,
    });
    fetchBoard();
  } catch (err) {
    setError('Failed to move card.');
  }
};

  if (error) return <p style={{ color: 'red' }}>{error}</p>;
  if (!board) return <p>Loading board...</p>;

  return (
    <div className="page">

          <div className="board-header">
        <Link to="/boards" className="back-link">← Back to boards</Link>
              <div className="cursor-field-wrapper">
                <input
                  ref={boardTitleInputRef}
                  className="board-title board-title-input"
                  value={boardTitleInput}
                  onChange={(e) => {
                    handleBoardTitleChange(e.target.value);
                    throttledSendCursorUpdateRef.current('BOARD_TITLE', Number(boardId), e.target.selectionStart);
                  }}
                  onFocus={(e) => {
                    handleBoardTitleFocus();
                    sendCursorUpdate('BOARD_TITLE', Number(boardId), e.target.selectionStart);
                  }}
                  onBlur={() => {
                    handleBoardTitleBlur();
                    sendCursorClear('BOARD_TITLE', Number(boardId));
                  }}
                />
                {remoteCursors[`BOARD_TITLE:${boardId}`] && (
                  <RemoteCursorFlag
                    username={remoteCursors[`BOARD_TITLE:${boardId}`].username}
                    color={getAvatarColor(remoteCursors[`BOARD_TITLE:${boardId}`].username)}
                    left={measureCaretOffset(boardTitleInputRef.current, remoteCursors[`BOARD_TITLE:${boardId}`].charIndex)}
                  />
                )}
              </div>
            <button className="ghost" onClick={() => setShowMembers(!showMembers)}>
              {showMembers ? 'Hide Members' : 'Members'}
            </button>
             <span className={`ws-status ${wsConnected ? 'connected' : 'disconnected'}`}>
                <span className="ws-dot"></span>
              {wsConnected ? 'Live' : 'Reconnecting...'}
            </span>

            <div className="presence-stack">
              {activeUsers.slice(0, 4).map((username) => (
                <div
                  key={username}
                  className="presence-avatar"
                  style={{ backgroundColor: getAvatarColor(username) }}
                  title={username}
                >
                  {getInitials(username)}
                </div>
              ))}
              {activeUsers.length > 4 && (
                <div className="presence-avatar presence-avatar-overflow">
                  +{activeUsers.length - 4}
                </div>
              )}
            </div>

        </div>

        {notification && (
            <div className="notification-banner">{notification}</div>
          )}

      {showMembers && (
        <div className="members-panel">
          <div className="members-list">
            <h4>Members</h4>
            {members.map((m) => (
              <div key={m.id} className="member-item">
                <span>{m.user.username}</span>
                <span className={`role-badge ${m.role.toLowerCase()}`}>{m.role}</span>
              </div>
            ))}
          </div>

          {isAdmin && (
            <div className="requests-list">
              <h4>Pending Join Requests</h4>
              {pendingRequests.length === 0 && <p className="text-dim">No pending requests.</p>}
              {pendingRequests.map((r) => (
                <div key={r.id} className="request-item">
                  <span>{r.user.username}</span>
                  <div className="request-actions">
                    <button onClick={() => handleAcceptRequest(r.id)}>Accept</button>
                    <button className="danger" onClick={() => handleRejectRequest(r.id)}>Reject</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
      

      <form onSubmit={handleCreateList} className="new-list-form">
        <input
          type="text"
          placeholder="New list title"
          value={newListTitle}
          onChange={(e) => setNewListTitle(e.target.value)}
        />
        <button type="submit">Add List</button>
      </form>

      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
        <div className="board-lanes">
          {board.lists?.map((list , index) => (
            <ListColumn
              key={list.id}
              list={list}
              index={index}
               titleValue={listTitleInputs[list.id] ?? list.title}
              onTitleChange={handleListTitleChange}
              onTitleFocus={handleListTitleFocus}
              onTitleBlur={handleListTitleBlur}
              titleInputRefs={listTitleInputRefs}
              remoteCursor={remoteCursors[`LIST_TITLE:${list.id}`]}
              onCursorMove={(charIndex) => throttledSendCursorUpdateRef.current('LIST_TITLE', list.id, charIndex)}
              onCursorFocus={(charIndex) => sendCursorUpdate('LIST_TITLE', list.id, charIndex)}
              onCursorClear={() => sendCursorClear('LIST_TITLE', list.id)}
              cardTitleInputs={cardTitleInputs}
              cardTitleInputRefs={cardTitleInputRefs}
              remoteCursors={remoteCursors}
              onCardTitleChange={handleCardTitleChange}
              onCardTitleFocus={handleCardTitleFocus}
              onCardTitleBlur={handleCardTitleBlur}
              onCardCursorMove={(cardId, charIndex) => throttledSendCursorUpdateRef.current('CARD_TITLE', cardId, charIndex)}
              onCardCursorFocus={(cardId, charIndex) => sendCursorUpdate('CARD_TITLE', cardId, charIndex)}
              onCardCursorClear={(cardId) => sendCursorClear('CARD_TITLE', cardId)}
              newCardTitles={newCardTitles}
              setNewCardTitles={setNewCardTitles}
              handleCreateCard={handleCreateCard}
              handleDeleteList={handleDeleteList}
              handleDeleteCard={handleDeleteCard}
               onOpenCard={setOpenCard}
               isAdmin={isAdmin}
            />
          ))}
        </div>
      </DndContext>
      {openCard && (
        <CardModal
          card={openCard}
          listId={openCard.listId}
          wsCharQueue={charQueue}
          wsConnected={wsConnected}
          stompClient={wsConnected ? clientRef.current : null}
          boardId={Number(boardId)}
          remoteDescriptionCursor={remoteCursors[`CARD_DESCRIPTION:${openCard.id}`]}
          onClose={() => setOpenCard(null)}
          onTitleSaved={(cardId, newTitle) => {
            setBoard((prev) => ({
              ...prev,
              lists: prev.lists.map((l) => ({
                ...l,
                cards: l.cards?.map((c) => (c.id === cardId ? { ...c, title: newTitle } : c)),
              })),
            }));
            setOpenCard((oc) => (oc ? { ...oc, title: newTitle } : oc));
          }}
        />
      )}
    </div>
  );
}

export default BoardDetail;