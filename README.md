# kraskaska-runner
This application allows you to automatically restart specific command. Initially made for minecraft servers.  
Usage:  
```
Usage: kraskaska-runner options_list
Options:
    --command, -c -> Command to keep running while runner is alive (always required) { String }
    --restartIfGracefulExit, -r [1] -> Restart even if command ended with 0 code (1 = true, anything else = false) { Int }
    --webhookUrl, -w -> Webhook url to post restart warnings { String }
    --strikesAmount, -s [3] -> Strikes (non-graceful, non-zero exits) needed to exit from runner. Set to 0 to disable { Int }
    --strikesInterval, -i [900] -> Seconds need to pass since last strike before resetting strike count to 0 { Int }
    --treatZeroAsError -> Zero will act like it's crashed
    --help, -h -> Usage info
```
