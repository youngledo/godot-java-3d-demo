## GUT entry point - uses gut.gd directly, no GUI
## Run: godot --path . --headless --scene tests/test_gut_entry.tscn
extends Node

var gut = null

func _ready():
	# Load main scene so test nodes are available
	var main_scene = load("res://main.tscn")
	if main_scene:
		var instance = main_scene.instantiate()
		add_child(instance)
		await get_tree().process_frame
		await get_tree().process_frame
		await get_tree().process_frame

	# Load GUT core
	gut = load("res://addons/gut/gut.gd").new()
	add_child(gut)

	gut.add_directory("res://tests/", "test_", ".gd")
	gut.log_level = 2
	gut.include_subdirectories = true
	gut._ignore_pause_before_teardown = true
	gut.end_run.connect(_on_tests_finished)
	gut.test_scripts()

func _on_tests_finished():
	var pass_count = gut.get_pass_count()
	var fail_count = gut.get_fail_count()
	print("\n=== Test Summary ===")
	print("Passed: %d | Failed: %d" % [pass_count, fail_count])
	if fail_count > 0:
		print("TESTS FAILED")
	else:
		print("ALL TESTS PASSED")
	get_tree().quit()
