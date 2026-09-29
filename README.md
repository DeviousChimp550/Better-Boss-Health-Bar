# Better Boss Health Bar

A Runelite plugin that replaces the default boss health bar with a dynamic and customizable one.

## Features

- **Customization**: Customize the boss health bar to suit your needs! Change the color, style, and animation properties of the health bar to tailor your boss health bars.

- **Animation**: Interpolates boss health smoothly, flashes the bar on each hit, and a visual damage buffer that animates recent damage for a few ticks before draining.

- **HP Display**: Choose to display the boss health as a percentage, hitpoint value, or both!

- **Breakpoints**: Configure health bar breakpoints for bosses! To do so, type the boss name, a colon, and then the breakpoint percentages separated by commas. Boss names are not case sensitive.

```
Alchemical Hydra: 75, 50, 25
Yama: 66, 33
```

- **Styles**
  - *Old School* (default): A flat and basic health bar. Breakpoints are split into separate segments.
  - *Evolution*: A rounder health bar with a glossy gradient. Breakpoints are visual notches on the bar.