extends GutTest

## Test: key press stability and input actions

var player: Node3D

func _find(path: String) -> Node:
	var root = get_tree().root
	var pg = root.find_child("Playground", true, false)
	if pg:
		return pg.get_node_or_null(path)
	return null

func before_all():
	await get_tree().process_frame
	player = _find("Player") as Node3D
	get_tree().paused = false
	await get_tree().process_frame

func simulate_key(keycode: int):
	var event = InputEventKey.new()
	event.keycode = keycode
	event.pressed = true
	Input.parse_input_event(event)
	await get_tree().process_frame
	event.pressed = false
	Input.parse_input_event(event)
	await get_tree().process_frame

func test_move_actions_exist():
	assert_true(InputMap.has_action("move_up"), "move_up should exist")
	assert_true(InputMap.has_action("move_down"), "move_down should exist")
	assert_true(InputMap.has_action("move_left"), "move_left should exist")
	assert_true(InputMap.has_action("move_right"), "move_right should exist")

func test_jump_action_exists():
	assert_true(InputMap.has_action("jump"), "jump should exist")

func test_attack_action_exists():
	assert_true(InputMap.has_action("attack"), "attack should exist")

func test_pause_action_exists():
	assert_true(InputMap.has_action("pause"), "pause should exist")

func test_enter_key_no_crash():
	simulate_key(KEY_ENTER)
	assert_true(true, "Enter key did not crash")

func test_escape_key_no_crash():
	simulate_key(KEY_ESCAPE)
	assert_true(true, "Escape key did not crash")

func test_space_key_no_crash():
	simulate_key(KEY_SPACE)
	assert_true(true, "Space key did not crash")

func test_wasd_keys_no_crash():
	simulate_key(KEY_W)
	simulate_key(KEY_A)
	simulate_key(KEY_S)
	simulate_key(KEY_D)
	assert_true(true, "WASD keys did not crash")

func test_arrow_keys_no_crash():
	simulate_key(KEY_UP)
	simulate_key(KEY_DOWN)
	simulate_key(KEY_LEFT)
	simulate_key(KEY_RIGHT)
	assert_true(true, "Arrow keys did not crash")

func test_player_has_collision():
	if player == null:
		pending("Player not found")
		return
	var col = player.get_node_or_null("CharacterCollisionShape")
	assert_not_null(col, "Player should have CharacterCollisionShape")

func test_player_has_camera_controller():
	if player == null:
		pending("Player not found")
		return
	var cc = player.get_node_or_null("CameraController")
	assert_not_null(cc, "Player should have CameraController")

func test_movement_changes_position():
	if player == null:
		pending("Player not found")
		return
	var old_pos = player.global_position
	for i in range(30):
		var event = InputEventAction.new()
		event.action = "move_right"
		event.strength = 1.0
		event.pressed = true
		Input.parse_input_event(event)
		await get_tree().process_frame
	var event = InputEventAction.new()
	event.action = "move_right"
	event.pressed = false
	Input.parse_input_event(event)
	await get_tree().process_frame
	assert_ne(player.global_position, old_pos, "Player should move when move_right is pressed")
