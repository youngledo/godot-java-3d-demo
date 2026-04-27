extends GutTest

## Test: initial paused state (must run first before any test unpauses)

func _find(path: String) -> Node:
	var root = get_tree().root
	var pg = root.find_child("Playground", true, false)
	if pg:
		return pg.get_node_or_null(path)
	return null

func before_all():
	await get_tree().process_frame

func test_scene_starts_paused():
	assert_true(get_tree().paused, "Scene should be paused at startup")

func test_demo_page_exists_when_paused():
	var dp = _find("DemoPage")
	assert_not_null(dp, "DemoPage should exist while paused")
