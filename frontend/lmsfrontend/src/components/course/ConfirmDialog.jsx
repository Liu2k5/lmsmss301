import { useState } from 'react'

/** Hộp thoại xác nhận; onConfirm trả về Promise, dialog tự hiển thị trạng thái đang xử lý. */
export default function ConfirmDialog({ title, message, confirmLabel = 'Xác nhận', danger = true, onConfirm, onCancel }) {
  const [busy, setBusy] = useState(false)

  async function handleConfirm() {
    setBusy(true)
    try {
      await onConfirm()
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="modal modal--small" role="alertdialog" aria-modal="true" aria-labelledby="confirm-dialog-title">
        <header className="modal__header">
          <h2 id="confirm-dialog-title">{title}</h2>
        </header>
        <div className="modal__body">
          <p>{message}</p>
        </div>
        <footer className="modal__footer">
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={busy}>
            Huỷ
          </button>
          <button
            type="button"
            className={`button ${danger ? 'button--danger' : 'button--primary'}`}
            onClick={handleConfirm}
            disabled={busy}
          >
            {busy ? 'Đang xử lý…' : confirmLabel}
          </button>
        </footer>
      </div>
    </div>
  )
}
