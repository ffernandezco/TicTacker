<?php
require_once 'db_connect.php';
require_once 'jwt_utils.php';

header('Content-Type: application/json');

// Cargar JSON de cuenta de servicio
$serviceAccount = json_decode(file_get_contents('firebase_service_account.json'), true);

// Crear el JWT y obtener token OAuth
$jwt = create_jwt($serviceAccount['client_email'], $serviceAccount['private_key']);

$token_response = file_get_contents('https://oauth2.googleapis.com/token', false, stream_context_create([
    'http' => [
        'method' => 'POST',
        'header' => "Content-Type: application/x-www-form-urlencoded\r\n",
        'content' => http_build_query([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt
        ])
    ]
]));

$token_data = json_decode($token_response, true);
$access_token = $token_data['access_token'] ?? null;

if (!$access_token) {
    echo json_encode(["success" => false, "error" => "Error obteniendo token de acceso"]);
    exit;
}

// Obtener tokens de la base de datos
$query = "SELECT token FROM fcm_tokens";
$result = $con->query($query);
$tokens = [];

while ($row = $result->fetch_assoc()) {
    $tokens[] = $row['token'];
}
if (empty($tokens)) {
    echo json_encode(["success" => false, "error" => "No hay tokens registrados"]);
    exit;
}

// Crear y enviar mensajes individuales (uno por token)
$project_id = 'das-tictacker';
$fcm_url = "https://fcm.googleapis.com/v1/projects/$project_id/messages:send";

$headers = [
    "Authorization: Bearer $access_token",
    "Content-Type: application/json"
];

// Extraer datos desde la solicitud
$request_data = json_decode(file_get_contents('php://input'), true);

// Extraer título y mensaje de la solicitud
// Primero comprobamos si están en la raíz de la solicitud
$title = $request_data['title'] ?? null;
$body = $request_data['message'] ?? null;

// Si no están en la raíz, verificar si están en el objeto 'notification'
if (!$title && isset($request_data['notification']['title'])) {
    $title = $request_data['notification']['title'];
}
if (!$body && isset($request_data['notification']['body'])) {
    $body = $request_data['notification']['body'];
}

// Valores por defecto si no se encuentran
$title = $title ?? 'Sin título';
$body = $body ?? 'Sin contenido';

// Extraer datos para el payload
$data_payload = $request_data['data'] ?? [];

$results = [];
foreach ($tokens as $token) {
    $message = [
        'message' => [
            'token' => $token,
            'notification' => [
                'title' => $title,
                'body' => $body
            ],
            'data' => array_merge([
                'title' => $title,    // Añadir título y mensaje al payload de datos
                'body' => $body,      // para asegurar que esté disponible en ambos
                'click_action' => 'FLUTTER_NOTIFICATION_CLICK',
                'status' => 'done'
            ], $data_payload)
        ]
    ];

    $ch = curl_init($fcm_url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($message));

    $response = curl_exec($ch);
    $results[] = json_decode($response, true);
    curl_close($ch);
}

echo json_encode(["success" => true, "results" => $results]);
$con->close();
?>