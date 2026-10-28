param([string]$cmd, [int]$procId, [string]$arg = "")
# Window helper for OUR dev client. Only touches the window(s) of the ONE process id given (the client JVM that
# client.sh started and recorded); never searches by window title, so the user's own client is never captured.
#   win.ps1 list <pid>
#   win.ps1 shot <pid> <out.png>    PrintWindow capture of the client area (works while the window is covered);
#                                   exit 4 and a `blank ...` line when the image is one flat colour (see below)
#   win.ps1 blank 0 <file.png>      exit 4 and `blank ...` when an existing image is one flat colour, else `ok`
#   win.ps1 key <pid> <F1|F2|F3|ESC|T|ENTER|E|1..9>   post a key press to the window
#   win.ps1 mouse <pid> <RDOWN|RUP|LCLICK|CORNER>  post a mouse button to the window (RDOWN holds "use" until RUP: spyglass,
#                                   lens); CORNER moves the cursor to the top left (no tooltip in a screen shot)
#   win.ps1 close <pid>             ask the window to close (WM_CLOSE)
# Blank PrintWindow images (all white): PrintWindow copies what DWM composed for the window, and DWM composes nothing
# while the display is off (the power plan turns it off after 15 min without input; the monitor then goes to standby
# and drops off the bus: Kernel-PnP 1010 "DISPLAY\SAM7053 surprise removed", 2026-10-19 20:46:08) or the window is
# minimized. Reproduced 2026-10-20 15:06 with the user idle for 3.6 h. The game keeps rendering, so client.sh falls back
# to F2 (Minecraft's own framebuffer screenshot), which works with the display off.
Add-Type -ReferencedAssemblies System.Drawing @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
using System.Drawing;
public static class CaWin {
  public delegate bool EnumProc(IntPtr h, IntPtr p);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc f, IntPtr p);
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
  [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr dc, uint flags);
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint m, IntPtr w, IntPtr l);
  [DllImport("user32.dll")] public static extern uint MapVirtualKey(uint c, uint t);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
  [DllImport("user32.dll")] static extern bool GetLastInputInfo(ref LASTINPUT l);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
  [StructLayout(LayoutKind.Sequential)] struct LASTINPUT { public uint Size, Time; }
  // Minutes since the user's last keyboard/mouse input (the display turns off after the power plan's timeout).
  public static uint IdleMinutes() {
    var l = new LASTINPUT(); l.Size = (uint) Marshal.SizeOf(l);
    return GetLastInputInfo(ref l) ? ((uint) Environment.TickCount - l.Time) / 60000 : 0;
  }
  public static List<object[]> Windows(uint want) {
    var res = new List<object[]>();
    EnumWindows((h, p) => { uint pid; GetWindowThreadProcessId(h, out pid);
      if (pid == want && IsWindowVisible(h)) { var sb = new StringBuilder(256); GetWindowText(h, sb, 256); res.Add(new object[]{h, pid, sb.ToString()}); }
      return true; }, IntPtr.Zero);
    return res;
  }
  public static void Shot(IntPtr h, string path) {
    RECT r; GetClientRect(h, out r);
    using (var bmp = new Bitmap(Math.Max(1, r.R - r.L), Math.Max(1, r.B - r.T))) {
      using (var g = Graphics.FromImage(bmp)) { IntPtr dc = g.GetHdc(); PrintWindow(h, dc, 3); g.ReleaseHdc(dc); }
      bmp.Save(path, System.Drawing.Imaging.ImageFormat.Png);
    }
  }
  // "#RRGGBB (WxH)" when a 64 x 36 grid of samples spans at most 6 levels per channel and is near white or near black
  // (what a capture without composition gives), else null. Fog or a flat sky keeps its colour and is never blank.
  public static string Flat(string path) {
    using (var bmp = new Bitmap(path)) {
      int r0 = 255, g0 = 255, b0 = 255, r1 = 0, g1 = 0, b1 = 0;
      for (int i = 0; i < 64; i++) for (int j = 0; j < 36; j++) {
        Color c = bmp.GetPixel((int) ((i + 0.5) * bmp.Width / 64), (int) ((j + 0.5) * bmp.Height / 36));
        r0 = Math.Min(r0, c.R); g0 = Math.Min(g0, c.G); b0 = Math.Min(b0, c.B);
        r1 = Math.Max(r1, c.R); g1 = Math.Max(g1, c.G); b1 = Math.Max(b1, c.B);
      }
      if (r1 - r0 > 6 || g1 - g0 > 6 || b1 - b0 > 6) return null;
      bool white = r0 >= 245 && g0 >= 245 && b0 >= 245, black = r1 <= 10 && g1 <= 10 && b1 <= 10;
      if (!white && !black) return null;
      return String.Format("#{0:X2}{1:X2}{2:X2} ({3}x{4})", (r0 + r1) / 2, (g0 + g1) / 2, (b0 + b1) / 2, bmp.Width, bmp.Height);
    }
  }
  // The message at the client area's centre (lParam = y << 16 | x); GLFW takes the button from the message alone.
  public static void Mouse(IntPtr h, uint msg, uint wParam) {
    RECT r; GetClientRect(h, out r);
    PostMessage(h, msg, (IntPtr)wParam, (IntPtr)((((r.B - r.T) / 2) << 16) | ((r.R - r.L) / 2)));
  }
  public static void Key(IntPtr h, uint vk) {
    uint sc = MapVirtualKey(vk, 0);
    PostMessage(h, 0x0100, (IntPtr)vk, (IntPtr)(1 | (sc << 16)));
    System.Threading.Thread.Sleep(60);
    PostMessage(h, 0x0101, (IntPtr)vk, (IntPtr)(1 | (sc << 16) | (1u << 30) | (1u << 31)));
  }
}
"@
if ($cmd -eq 'blank') {
  $flat = [CaWin]::Flat($arg)
  if ($flat) { "blank $flat"; exit 4 }
  "ok"; exit 0
}
if ($procId -le 0) { throw "a process id is required" }
$wins = [CaWin]::Windows([uint32]$procId)
function Main() { foreach ($w in $wins) { if ($w[2] -ne "") { return $w[0] } }; throw "no window for pid $procId" }
$keys = @{ F1 = 0x70; F2 = 0x71; F3 = 0x72; ESC = 0x1B; T = 0x54; ENTER = 0x0D; E = 0x45 }
1..9 | ForEach-Object { $keys["$_"] = 0x30 + $_ }
switch ($cmd) {
  'list'  { foreach ($w in $wins) { "{0} pid={1} title={2}" -f $w[0], $w[1], $w[2] } }
  'shot'  {
    $h = Main
    [CaWin]::Shot($h, $arg)
    $flat = [CaWin]::Flat($arg)
    if ($flat) { "blank $flat minimized=$([CaWin]::IsIconic($h)) user-idle=$([CaWin]::IdleMinutes())min"; exit 4 }
    "saved $arg"
  }
  'key'   { if (-not $keys.ContainsKey($arg)) { throw "unknown key $arg" }; [CaWin]::Key((Main), [uint32]$keys[$arg]); "key $arg" }
  'mouse' {
    $h = Main
    switch ($arg) {
      'RDOWN'  { [CaWin]::Mouse($h, 0x0204, 0x0002) }
      'RUP'    { [CaWin]::Mouse($h, 0x0205, 0) }
      'CORNER' { [void][CaWin]::PostMessage($h, 0x0200, [IntPtr]::Zero, [IntPtr]0x40004) }
      'LCLICK' { [CaWin]::Mouse($h, 0x0201, 0x0001); Start-Sleep -Milliseconds 60; [CaWin]::Mouse($h, 0x0202, 0) }
      default  { throw "unknown mouse action $arg" }
    }
    "mouse $arg"
  }
  'close' { [void][CaWin]::PostMessage((Main), 0x0010, [IntPtr]::Zero, [IntPtr]::Zero); "close sent" }
  default { throw "usage: win.ps1 list|shot|key|mouse|close <pid> [arg]" }
}
