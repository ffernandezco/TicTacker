<?php
require_once 'db_connect.php'; # Conexión MySQL

header('Content-Type: application/json');

$data = json_decode(file_get_contents('php://input'), true);
$action = $data['action'];

$response = ["success" => false, "data" => []];

# Operaciones sobre la tabla de ajustes

if ($action == 'save') {
    # Guardar configuraciones
    // Verificar que los datos requeridos están presentes
    if (!isset($data['weekly_hours']) || !isset($data['working_days'])) {
        $response["error"] = "Faltan parámetros";
        echo json_encode($response);
        exit;
    }

    $weeklyHours = floatval($data['weekly_hours']);
    $workingDays = intval($data['working_days']);
    $reminderEnabled = intval($data['reminder_enabled']);
    $reminderHour = intval($data['reminder_hour']);
    $reminderMinute = intval($data['reminder_minute']);

    // Verificar si ya existe una configuración
    $checkQuery = "SELECT id FROM settings LIMIT 1";
    $checkResult = $con->query($checkQuery);

    if ($checkResult->num_rows > 0) {
        $query = "UPDATE settings SET weekly_hours = ?, working_days = ?, reminder_enabled = ?, reminder_hour = ?, reminder_minute = ? WHERE id = 1";
    } else {
        $query = "INSERT INTO settings (weekly_hours, working_days, reminder_enabled, reminder_hour, reminder_minute) VALUES (?, ?, ?, ?, ?)";
    }

    $stmt = $con->prepare($query);
    $stmt->bind_param("diiii", $weeklyHours, $workingDays, $reminderEnabled, $reminderHour, $reminderMinute);
    $response["success"] = $stmt->execute();
    $stmt->close();
} elseif ($action == 'get') {
      # Obtener configuraciones guardadas
      $query = "SELECT weekly_hours, working_days, reminder_enabled, reminder_hour, reminder_minute FROM settings LIMIT 1";
      $result = $con->query($query);

      if ($result->num_rows > 0) {
          $row = $result->fetch_assoc();
          $response["data"] = [
              "weekly_hours" => floatval($row['weekly_hours']),
              "working_days" => intval($row['working_days']),
              "reminder_enabled" => intval($row['reminder_enabled']),
              "reminder_hour" => intval($row['reminder_hour']),
              "reminder_minute" => intval($row['reminder_minute'])
          ];
      } else {
          // Valores por defecto si no hay configuración
          $response["data"] = [
              "weekly_hours" => 40.0,
              "working_days" => 5,
              "reminder_enabled" => 0,
              "reminder_hour" => 9,
              "reminder_minute" => 0
          ];
      }
      $response["success"] = true;
  }

echo json_encode($response);
$con->close();
?>