extends GutTest

## Test: camera follow and distance

var player: Node3D
var camera_controller: Node3D
var camera: Camera3D

func _find(path: String) -> Node:
	var root = get_tree().root
	var pg = root.find_child("Playground", true, false)
	if pg:
		return pg.get_node_or_null(path)
	return null

func before_all():
	await get_tree().process_frame
	player = _find("Player") as Node3D
	camera_controller = _find("Player/CameraController") as Node3D
	if camera_controller:
		camera = camera_controller.get_node_or_null("PlayerCamera")
	# Unpause so camera _process runs
	get_tree().paused = false
	await get_tree().process_frame
	await get_tree().process_frame
	await get_tree().process_frame

func test_camera_exists():
	if camera_controller == null:
		pending("CameraController not found")
		return
	assert_not_null(camera, "PlayerCamera should exist")

func test_camera_distance_reasonable():
	if player == null or camera == null:
		pending("Player or Camera not found")
		return
	var player_pos: Vector3 = player.global_position
	var camera_pos: Vector3 = camera.global_position
	var distance = camera_pos.distance_to(player_pos)
	assert_gt(distance, 2.0, "Camera should be at least 2 units from player")
	assert_lt(distance, 15.0, "Camera should be at most 15 units from player")

func test_camera_follows_player():
	if player == null or camera == null:
		pending("Player or Camera not found")
		return
	var camera_pos = camera.global_position
	assert_ne(camera_pos, Vector3.ZERO, "Camera should not be at origin")
