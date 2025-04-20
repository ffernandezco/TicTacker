<?php
require_once 'db_connect.php';

header('Content-Type: application/json');

$data = json_decode(file_get_contents('php://input'), true);

if (!isset($data['title']) || !isset($data['message'])) {
    echo json_encode(['success' => false, 'error' => 'Falta algún campo']);
    exit;
}

// Recoger todos los tokens
$query = "SELECT token FROM fcm_tokens";
$result = $con->query($query);

$tokens = [];
while ($row = $result->fetch_assoc()) {
    $tokens[] = $row['token'];
}

if (empty($tokens)) {
    echo json_encode(['success' => false, 'error' => 'No se ha encontrado ningún token']);
    exit;
}

// Enviar mensaje FCM
$fcmMessage = [
    'registration_ids' => $tokens,
    'notification' => [
        'title' => $data['title'],
        'body' => $data['message'],
        'sound' => 'default',
    ],
    'data' => [
        'click_action' => 'FLUTTER_NOTIFICATION_CLICK',
        'id' => '1',
        'status' => 'done'
    ]
];

// API Key de Firebase
$serverKey = 'AIzaSyA9Wqc4RbFu7lNIbwXB3TBpjM_aKg6b7Fo';

// Enviar notificación FCM
$ch = curl_init('https://fcm.googleapis.com/fcm/send');
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: key=' . $serverKey
]);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fcmMessage));

$response = curl_exec($ch);
curl_close($ch);

// Procesar respuesta
$responseData = json_decode($response, true);
echo json_encode([
    'success' => $responseData['success'] > 0,
    'results' => $responseData
]);

$con->close();
?>