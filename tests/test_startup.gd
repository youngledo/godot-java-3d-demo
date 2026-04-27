extends GutTest

## Test: critical nodes exist after scene loads

var player: Node3D
var camera_controller: Node3D
var demo_page: Node

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
	demo_page = _find("DemoPage")

func test_player_exists():
	assert_not_null(player, "Player node should exist")

func test_camera_controller_exists():
	assert_not_null(camera_controller, "CameraController should exist")

func test_demo_page_exists():
	assert_not_null(demo_page, "DemoPage should exist")

func test_camera_has_player_camera():
	if camera_controller == null:
		pending("CameraController not found")
		return
	var camera = camera_controller.get_node_or_null("PlayerCamera")
	assert_not_null(camera, "PlayerCamera should exist")
