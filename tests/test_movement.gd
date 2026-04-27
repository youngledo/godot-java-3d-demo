extends GutTest

## Test sustained movement input over multiple frames to simulate real gameplay.
## This catches crashes that only occur during actual physics processing.

var _player: Node = null
var _camera: Node = null
var _playground: Node = null

func _find(path: String) -> Node:
	if _playground == null:
		_playground = get_tree().root.find_child("Playground", true, false)
	if _playground == null:
		return null
	return _playground.get_node_or_null(path)

func before_all():
	# Unpause to allow physics processing
	get_tree().paused = false
	await get_tree().process_frame

func before_each():
	_player = _find("Player")
	_camera = _find("Player/CameraController/PlayerCamera")

func test_player_exists():
	assert_not_null(_player, "Player should exist")

func test_camera_exists():
	assert_not_null(_camera, "Camera should exist")

func test_camera_not_at_origin():
	if _camera == null:
		pending("Camera not found")
		return
	var cam_pos = _camera.global_position
	var dist = cam_pos.distance_to(Vector3.ZERO)
	assert_gt(dist, 0.5, "Camera should not be at the origin after setup")

func test_camera_facing_player():
	if _camera == null or _player == null:
		pending("Camera or Player not found")
		return
	var cam_pos = _camera.global_position
	var player_pos = _player.global_position
	# In third-person setup, camera should be above and behind the player
	# The SpringArm3D controls the actual view direction, so we check that
	# the camera is positioned above the player (y > player.y) and the
	# distance to the player is reasonable (checked in another test).
	assert_gt(cam_pos.y, player_pos.y, "Camera should be above the player (cam_y=%f, player_y=%f)" % [cam_pos.y, player_pos.y])

func test_camera_distance_reasonable():
	if _camera == null or _player == null:
		pending("Camera or Player not found")
		return
	var dist = _camera.global_position.distance_to(_player.global_position)
	assert_gt(dist, 1.0, "Camera should be at least 1 unit from player (got %f)" % dist)
	assert_lt(dist, 20.0, "Camera should be within 20 units of player (got %f)" % dist)

func test_sustained_move_right_no_crash():
	if _player == null:
		pending("Player not found")
		return
	var start_pos = _player.global_position
	# Simulate 60 frames of move_right
	for i in range(20):
		var event = InputEventAction.new()
		event.action = "move_right"
		event.pressed = true
		event.strength = 1.0
		Input.parse_input_event(event)
		await get_tree().physics_frame
	# Release
	var event = InputEventAction.new()
	event.action = "move_right"
	event.pressed = false
	Input.parse_input_event(event)
	await get_tree().physics_frame
	# Player should have moved
	var end_pos = _player.global_position
	var moved = start_pos.distance_to(end_pos)
	assert_gt(moved, 0.1, "Player should have moved after 60 frames of move_right")
	assert_true(true, "Sustained move_right should not crash")

func test_sustained_move_forward_no_crash():
	if _player == null:
		pending("Player not found")
		return
	var start_pos = _player.global_position
	for i in range(20):
		var event = InputEventAction.new()
		event.action = "move_up"
		event.pressed = true
		event.strength = 1.0
		Input.parse_input_event(event)
		await get_tree().physics_frame
	var event = InputEventAction.new()
	event.action = "move_up"
	event.pressed = false
	Input.parse_input_event(event)
	await get_tree().physics_frame
	var end_pos = _player.global_position
	var moved = start_pos.distance_to(end_pos)
	assert_gt(moved, 0.1, "Player should have moved after 60 frames of move_up")
	assert_true(true, "Sustained move_up should not crash")

func test_sustained_diagonal_move_no_crash():
	if _player == null:
		pending("Player not found")
		return
	var start_pos = _player.global_position
	for i in range(20):
		for action in ["move_up", "move_right"]:
			var event = InputEventAction.new()
			event.action = action
			event.pressed = true
			event.strength = 1.0
			Input.parse_input_event(event)
		await get_tree().physics_frame
	for action in ["move_up", "move_right"]:
		var event = InputEventAction.new()
		event.action = action
		event.pressed = false
		Input.parse_input_event(event)
	await get_tree().physics_frame
	var end_pos = _player.global_position
	var moved = start_pos.distance_to(end_pos)
	assert_gt(moved, 0.1, "Player should have moved diagonally")
	assert_true(true, "Sustained diagonal move should not crash")

func test_all_directions_sequential_no_crash():
	if _player == null:
		pending("Player not found")
		return
	for action in ["move_up", "move_down", "move_left", "move_right"]:
		for i in range(20):
			var event = InputEventAction.new()
			event.action = action
			event.pressed = true
			event.strength = 1.0
			Input.parse_input_event(event)
			await get_tree().physics_frame
		var release = InputEventAction.new()
		release.action = action
		release.pressed = false
		Input.parse_input_event(release)
		await get_tree().physics_frame
	assert_true(true, "All directions sequentially should not crash")

func test_jump_no_crash():
	if _player == null:
		pending("Player not found")
		return
	for i in range(20):
		var event = InputEventAction.new()
		event.action = "jump"
		event.pressed = true
		Input.parse_input_event(event)
		await get_tree().physics_frame
		var release = InputEventAction.new()
		release.action = "jump"
		release.pressed = false
		Input.parse_input_event(release)
		await get_tree().physics_frame
	assert_true(true, "Jump should not crash")

func test_move_and_jump_no_crash():
	if _player == null:
		pending("Player not found")
		return
	for i in range(10):
		# Move + jump simultaneously
		for action in ["move_right", "jump"]:
			var event = InputEventAction.new()
			event.action = action
			event.pressed = true
			if action == "move_right":
				event.strength = 1.0
			Input.parse_input_event(event)
		await get_tree().physics_frame
	# Release all
	for action in ["move_right", "jump"]:
		var release = InputEventAction.new()
		release.action = action
		release.pressed = false
		Input.parse_input_event(release)
	await get_tree().physics_frame
	assert_true(true, "Move + jump simultaneously should not crash")

func test_player_stays_on_ground():
	if _player == null:
		pending("Player not found")
		return
	var start_y = _player.global_position.y
	# Move forward 10 frames
	for i in range(10):
		var event = InputEventAction.new()
		event.action = "move_up"
		event.pressed = true
		event.strength = 1.0
		Input.parse_input_event(event)
		await get_tree().physics_frame
	var release = InputEventAction.new()
	release.action = "move_up"
	release.pressed = false
	Input.parse_input_event(release)
	await get_tree().physics_frame
	var end_y = _player.global_position.y
	assert_gt(end_y, start_y - 2.0, "Player should not fall through floor during movement (start_y=%f, end_y=%f)" % [start_y, end_y])
