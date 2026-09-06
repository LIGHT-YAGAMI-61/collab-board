// Measures how far into an input's text a given character index sits,
// in pixels, by drawing that substring on an offscreen canvas with the
// same font. Used to position another user's cursor flag.
export function measureCaretOffset(inputEl, charIndex) {
  if (!inputEl) return 0;
  const style = getComputedStyle(inputEl);
  const canvas = measureCaretOffset._canvas || (measureCaretOffset._canvas = document.createElement('canvas'));
  const ctx = canvas.getContext('2d');
  ctx.font = `${style.fontStyle} ${style.fontWeight} ${style.fontSize} ${style.fontFamily}`;
  const textWidth = ctx.measureText(inputEl.value.slice(0, charIndex)).width;
  const paddingLeft = parseFloat(style.paddingLeft) || 0;
  return paddingLeft + textWidth;
}

// Calls fn at most once per `delay` ms, always firing the trailing call
// so the last keystroke's position is never dropped.
export function throttle(fn, delay) {
  let last = 0;
  let timeoutId = null;
  return (...args) => {
    const now = Date.now();
    const remaining = delay - (now - last);
    if (remaining <= 0) {
      clearTimeout(timeoutId);
      last = now;
      fn(...args);
    } else {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => {
        last = Date.now();
        fn(...args);
      }, remaining);
    }
  };
}