// (c) 2006-2010, Thor Magnusson - www.ixi-audio.net
// GNU license - google it.

MIDIKeyboardView : UserView {

	var <>keys;
	var trackKey, chosenkey, mouseTracker;
	var octaves, startnote;
	var downAction, upAction, trackAction;

	*new { arg w, bounds, octaves, startnote;
		bounds = bounds ? Rect(20, 10, 364, 60);
		^super.new(w, bounds).initMIDIKeyboard(octaves, startnote);
	}

	initMIDIKeyboard { arg argoctaves=3, argstartnote;
		var r, pen;
 		var bounds = this.bounds;
		octaves = argoctaves ? 4;

		pen	= Pen;

		startnote = argstartnote ? 48;
		trackKey = 0;
		keys = List.new;

		octaves.do({arg j;
			12.do({arg i;
				var note = startnote+i+(j*12);
				keys.add(MIDIKey.new(note, MIDIKey.rect(bounds, octaves, j, note), MIDIKey.color(note)));
			});
		});

		this
			.minSize_(Point(bounds.width, bounds.height))
			.canFocus_(false)
			//.relativeOrigin_(false)
			.mouseDownAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				trackKey = chosenkey;
				chosenkey.color = Color.grey;
				downAction.value(chosenkey.note);
				this.refresh;
			})
			.mouseMoveAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				if(trackKey.note != chosenkey.note, {
					trackKey.color = trackKey.scalecolor; // was : type
					trackKey = chosenkey;
					chosenkey.color = Color.grey;
					trackAction.value(chosenkey.note);
					this.refresh;
				});
			})
			.mouseUpAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				trackKey = chosenkey;
				chosenkey.color = chosenkey.scalecolor; // was:  type
				upAction.value(chosenkey.note);
				this.refresh;
			})
			.drawFunc_({
				var bounds = this.bounds;
				octaves.do({arg j;
					// first draw the white keys
					12.do({arg i;
						var key, rect;
						key = keys[i+(j*12)];
						rect = key.updateRect(bounds, octaves, j);
						if(key.type == Color.white, {
							pen.color = Color.black;
							pen.strokeRect(Rect(rect.left+0.5, rect.top+0.5, rect.width+0.5, rect.height-0.5));
							pen.color = key.color; // white or grey
							pen.fillRect(Rect(rect.left+0.5, rect.top+0.5, rect.width+0.5, rect.height-0.5));
						});
					});
					// and then draw the black keys on top of the white
					12.do({arg i;
						var key, rect;
						key = keys[i+(j*12)];
						rect = key.updateRect(bounds, octaves, j);
						if(key.type == Color.black, {
							pen.color = Color.black;
							pen.strokeRect(Rect(rect.left+0.5, rect.top+0.5, rect.width, rect.height));
							pen.color = key.color;
							pen.fillRect(Rect(rect.left+0.5, rect.top+0.5, rect.width, rect.height));
						});
					})
				})
			});
	}

	keyDown { arg note, color; // midinote
		if(note.isArray, {
			note.do({arg note;
				if(this.inRange(note), {
					keys[note - startnote].color = Color.grey;
				});
			});
		}, {
			if(this.inRange(note), {
				keys[note - startnote].color = Color.grey;
			});
		});
		this.refresh;
	}

	keyUp { arg note; // midinote
//		if(this.inRange(note), {
//			keys[note - startnote].color = keys[note - startnote].scalecolor;
//		});
		if(note.isArray, {
			note.do({arg note;
				if(this.inRange(note), {
					keys[note - startnote].color = keys[note - startnote].scalecolor;
				});
			});
		}, {
			if(this.inRange(note), {
				keys[note - startnote].color = keys[note - startnote].scalecolor;
			});
		});
		this.refresh;
	}

	keyDownAction_ { arg func;
		downAction = func;
	}

	keyUpAction_ { arg func;
		upAction = func;
	}

	keyTrackAction_ { arg func;
		trackAction = func;
	}

	showScale {arg argscale, key=startnote, argcolor;
		var color, scale, counter, transp;
		this.clear; // erase scalecolors (make them their type)
		counter = 0;
		color = argcolor ? Color.red;
		transp = key%12;
		scale = argscale + transp + startnote;
		keys.do({arg key, i;
			key.color = key.type; // back to original color
			if(((i-transp)%12 == 0)&&((i-transp)!=0), { counter = 0; scale = scale+12;});			if(key.note == scale[counter], {
				counter = counter + 1;
				key.color = key.color.blend(color, 0.5);
				key.scalecolor = key.color;
				key.inscale = true;
			});
		});
		this.refresh;
	}

	clear {
		keys.do({arg key, i;
			key.color = key.type; // back to original color
			key.scalecolor = key.color;
			key.inscale = false;
		});
		this.refresh;
	}

	// just for fun
	playScale { arg argscale, key=startnote, int=0.5;
		var scale = argscale;
		SynthDef(\midikeyboardsine, {arg freq, amp = 0.25;
			Out.ar(0, (SinOsc.ar(freq,0,amp)*EnvGen.ar(Env.perc, doneAction:2)).dup)
		}).load(Server.default);
		Task({
			scale.mirror.do({arg note;
				Synth(\midikeyboardsine, [\freq, (key+note).midicps]);
				int.wait;
			});		}).start;
	}

	setColor {arg note, color;

		var newcolor = keys[note - startnote].color.blend(color, 0.5);
		keys[note - startnote].color = newcolor;
		keys[note - startnote].scalecolor = newcolor;
		this.refresh;
	}

	getColor { arg note;
		^keys[note - startnote].color;
	}

	getType { arg note;
		^keys[note - startnote].type;
	}


	removeColor {arg note;
		keys[note - startnote].scalecolor = keys[note - startnote].type;
		keys[note - startnote].color = keys[note - startnote].type;
		this.refresh;
	}

	inScale {arg key;
		^keys[key-startnote].inscale;
	}

	retNote {arg key;
		^keys[key].note;
	}

	// local function
	findNote {arg x, y;
		var chosenkeys, bounds;
		chosenkeys = [];
		bounds = this.bounds;
		keys.reverse.do({arg key;
			var rect = key.updateRect(bounds, octaves, ((key.note - startnote) / 12).floor.asInteger);
			if(rect.containsPoint(Point.new(x,y)), {
				chosenkeys = chosenkeys.add(key);
			});
		});
		block{|break|
			chosenkeys.do({arg key;
				if(key.type == Color.black, {
					chosenkey = key;
					break.value; // the important part
				}, {
					chosenkey = key;
				});
			});
		};
		^chosenkey;
	}

	// local
	inRange {arg note; // check if an input note is in the range of the keyboard
		if((note>startnote) && (note<(startnote + (octaves*12))), {^true}, {^false});
	}

}
