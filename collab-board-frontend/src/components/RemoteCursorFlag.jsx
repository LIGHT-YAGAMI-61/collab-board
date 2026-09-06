function RemoteCursorFlag({ username, color, left }) {
  return (
    <div className="remote-cursor-flag" style={{ left }}>
      <div className="remote-cursor-caret" style={{ backgroundColor: color }} />
      <div className="remote-cursor-name" style={{ backgroundColor: color }}>{username}</div>
    </div>
  );
}

export default RemoteCursorFlag;