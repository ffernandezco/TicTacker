<?php
require_once 'db_connect.php';

header('Content-Type: application/json');

$data = json_decode(file_get_contents('php://input'), true);
$action = $data['action'];
$username = isset($data['username']) ? $data['username'] : '';

$response = ["success" => false, "data" => []];

switch ($action) {
    case 'insert':
        $query = "INSERT INTO fichajes (fecha, hora_entrada, latitud, longitud, username) VALUES (?, ?, ?, ?, ?)";
        $stmt = $con->prepare($query);
        $stmt->bind_param("ssdds",
            $data['fecha'],
            $data['hora_entrada'],
            $data['latitud'],
            $data['longitud'],
            $username);
        $response["success"] = $stmt->execute();
        if ($response["success"]) {
            $response["data"] = ["id" => $stmt->insert_id];
        }
        break;

    case 'update':
        $query = "UPDATE fichajes SET hora_salida = ?, latitud = ?, longitud = ? WHERE id = ? AND username = ?";
        $stmt = $con->prepare($query);
        $horaSalida = !empty($data['hora_salida']) ? $data['hora_salida'] : null;
        $stmt->bind_param("sddis",
            $horaSalida,
            $data['latitud'],
            $data['longitud'],
            $data['id'],
            $username);
        $response["success"] = $stmt->execute();
        break;

    case 'get_all':
        $query = "SELECT id, fecha, hora_entrada, hora_salida, latitud, longitud, username
                  FROM fichajes
                  WHERE username = ?
                  ORDER BY fecha DESC, hora_entrada DESC";
        $stmt = $con->prepare($query);
        $stmt->bind_param("s", $username);
        $stmt->execute();
        $result = $stmt->get_result();

        $fichajes = [];
        while ($row = $result->fetch_assoc()) {
            $fichajes[] = [
                "id" => $row['id'],
                "fecha" => $row['fecha'],
                "hora_entrada" => $row['hora_entrada'],
                "hora_salida" => $row['hora_salida'],
                "latitud" => $row['latitud'],
                "longitud" => $row['longitud'],
                "username" => $row['username']
            ];
        }
        $response["data"] = $fichajes;
        $response["success"] = true;
        break;

    case 'get_today':
        $query = "SELECT id, fecha, hora_entrada, hora_salida, latitud, longitud, username
                  FROM fichajes
                  WHERE fecha = ? AND username = ?
                  ORDER BY hora_entrada ASC";
        $stmt = $con->prepare($query);
        $stmt->bind_param("ss", $data['fecha'], $username);
        $stmt->execute();
        $result = $stmt->get_result();

        $fichajes = [];
        while ($row = $result->fetch_assoc()) {
            $fichajes[] = [
                "id" => $row['id'],
                "fecha" => $row['fecha'],
                "hora_entrada" => $row['hora_entrada'],
                "hora_salida" => $row['hora_salida'],
                "latitud" => $row['latitud'],
                "longitud" => $row['longitud'],
                "username" => $row['username']
            ];
        }
        $response["data"] = $fichajes;
        $response["success"] = true;
        break;

    case 'get_last':
        $query = "SELECT id, fecha, hora_entrada, hora_salida, latitud, longitud, username
                  FROM fichajes
                  WHERE fecha = ? AND username = ?
                  ORDER BY hora_entrada DESC LIMIT 1";
        $stmt = $con->prepare($query);
        $stmt->bind_param("ss", $data['fecha'], $username);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows > 0) {
            $row = $result->fetch_assoc();
            $response["data"] = [
                "id" => $row['id'],
                "fecha" => $row['fecha'],
                "hora_entrada" => $row['hora_entrada'],
                "hora_salida" => $row['hora_salida'],
                "latitud" => $row['latitud'],
                "longitud" => $row['longitud'],
                "username" => $row['username']
            ];
            $response["success"] = true;
        }
        break;

    case 'delete_all':
        $query = "DELETE FROM fichajes WHERE username = ?";
        $stmt = $con->prepare($query);
        $stmt->bind_param("s", $username);
        $response["success"] = $stmt->execute();
        break;

    case 'update_complete':
        $query = "UPDATE fichajes SET hora_entrada = ?, hora_salida = ?, latitud = ?, longitud = ? WHERE id = ? AND username = ?";
        $stmt = $con->prepare($query);
        $horaSalida = !empty($data['hora_salida']) ? $data['hora_salida'] : null;
        $stmt->bind_param("ssddis",
            $data['hora_entrada'],
            $horaSalida,
            $data['latitud'],
            $data['longitud'],
            $data['id'],
            $username);
        $response["success"] = $stmt->execute();
        break;
}

echo json_encode($response);
if (isset($stmt)) $stmt->close();
$con->close();
?>