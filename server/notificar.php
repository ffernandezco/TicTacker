<?php
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>TicTacker</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.5/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-SgOJa3DmI69IUzQ2PVdRZhwQ+dy64/BUtbMJw1MZ8t5HZApcHrRKUc4W0kG879m7" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.5/dist/js/bootstrap.bundle.min.js" integrity="sha384-k6d4wzSIapyDyv1kpU366/PK5hCdSbCRGRCMv+eplOQJWyd1fbcAu9OCUj5zNLiq" crossorigin="anonymous"></script>
    <style>
        body { padding: 2rem; background-color: #f8f9fa; }
        .card { max-width: 600px; margin: auto; }
    </style>
</head>
<body>
    <div class="card shadow">
        <div class="card-header bg-primary text-white">
            <h4 class="mb-0">Enviar notificación</h4>
        </div>
        <div class="card-body">
            <form id="notificationForm">
                <div class="mb-3">
                    <label class="form-label">Título</label>
                    <input type="text" class="form-control" name="title" required>
                </div>
                <div class="mb-3">
                    <label class="form-label">Mensaje</label>
                    <textarea class="form-control" name="message" rows="3" required></textarea>
                </div>
                <div class="d-grid">
                    <button type="submit" class="btn btn-success">Enviar notificación</button>
                </div>
            </form>
            <div id="responseBox" class="mt-4 d-none alert"></div>
        </div>
    </div>

    <script>
        const form = document.getElementById('notificationForm');
        const responseBox = document.getElementById('responseBox');

        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const title = form.title.value.trim();
            const message = form.message.value.trim();

            const payload = {
                title: title,
                message: message,
                data: {
                    status: 'done',
                    click_action: 'FLUTTER_NOTIFICATION_CLICK'
                }
            };

            const res = await fetch('send_notification_v1.php', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });

            const result = await res.json();
            responseBox.classList.remove('d-none');
            if (result.success) {
                responseBox.className = 'alert alert-success';
                responseBox.textContent = 'Notificación enviada correctamente';
            } else {
                responseBox.className = 'alert alert-danger';
                responseBox.textContent = 'Error: ' + (result.error || 'No se pudo enviar la notificación');
            }
        });
    </script>
</body>
</html>
