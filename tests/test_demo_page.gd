extends GutTest

## Test: DemoPage resume and pause toggle

var demo_page: Node

func _find(path: String) -> Node:
	var root = get_tree().root
	var pg = root.find_child("Playground", true, false)
	if pg:
		return pg.get_node_or_null(path)
	return null

func before_all():
	await get_tree().process_frame
	demo_page = _find("DemoPage")

func test_resume_button_exists():
	if demo_page == null:
		pending("DemoPage not found")
		return
	var btn = demo_page.get_node_or_null("CanvasLayer/DemoPageRoot/Content/MarginContainer/Buttons/Resume")
	assert_not_null(btn, "Resume button should exist")

func test_resume_button_unpauses():
	if demo_page == null:
		pending("DemoPage not found")
		return
	# Ensure paused first
	get_tree().paused = true
	await get_tree().process_frame
	var resume_btn = demo_page.get_node_or_null("CanvasLayer/DemoPageRoot/Content/MarginContainer/Buttons/Resume")
	if resume_btn == null:
		pending("Resume button not found")
		return
	resume_btn.emit_signal("pressed")
	await get_tree().process_frame
	await get_tree().process_frame
	assert_false(get_tree().paused, "Game should be unpaused after Resume")

func test_pause_key_toggles():
	get_tree().paused = false
	await get_tree().process_frame
	var event = InputEventAction.new()
	event.action = "pause"
	event.pressed = true
	Input.parse_input_event(event)
	await get_tree().process_frame
	await get_tree().process_frame
	assert_true(get_tree().paused, "Game should pause on pause key")
