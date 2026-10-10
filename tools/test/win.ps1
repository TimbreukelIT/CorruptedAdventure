param([string]$cmd, [int]$procId, [string]$arg = "")
# Window helper for OUR dev client. Only touches the window(s) of the ONE process id given (the client JVM that
# client.sh started and recorded); never searches by window title, so the user's own client is never captured.
#   win.ps1 list <pid>
#   win.ps1 shot <pid> <out.png>    PrintWindow capture of the client area (works while the window is covered)
#   win.ps1 key <pid> <F1|F2|F3|ESC|T|ENTER>   post a key press to the window
#   win.ps1 close <pid>             ask the window to close (WM_CLOSE)
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
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
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
  public static void Key(IntPtr h, uint vk) {
    uint sc = MapVirtualKey(vk, 0);
    PostMessage(h, 0x0100, (IntPtr)vk, (IntPtr)(1 | (sc << 16)));
    System.Threading.Thread.Sleep(60);
    PostMessage(h, 0x0101, (IntPtr)vk, (IntPtr)(1 | (sc << 16) | (1u << 30) | (1u << 31)));
  }
}
"@
if ($procId -le 0) { throw "a process id is required" }
$wins = [CaWin]::Windows([uint32]$procId)
function Main() { foreach ($w in $wins) { if ($w[2] -ne "") { return $w[0] } }; throw "no window for pid $procId" }
$keys = @{ F1 = 0x70; F2 = 0x71; F3 = 0x72; ESC = 0x1B; T = 0x54; ENTER = 0x0D }
switch ($cmd) {
  'list'  { foreach ($w in $wins) { "{0} pid={1} title={2}" -f $w[0], $w[1], $w[2] } }
  'shot'  { [CaWin]::Shot((Main), $arg); "saved $arg" }
  'key'   { if (-not $keys.ContainsKey($arg)) { throw "unknown key $arg" }; [CaWin]::Key((Main), [uint32]$keys[$arg]); "key $arg" }
  'close' { [void][CaWin]::PostMessage((Main), 0x0010, [IntPtr]::Zero, [IntPtr]::Zero); "close sent" }
  default { throw "usage: win.ps1 list|shot|key|close <pid> [arg]" }
}
